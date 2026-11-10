package com.collaboffice.doc.service;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.impl.CellServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import io.minio.*;
import io.minio.http.Method;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service @RequiredArgsConstructor
public class SheetTransferService {
    private final DocumentService documents;
    private final SheetSheetMapper sheets;
    private final CellServiceImpl cells;
    private final SheetWorkbookService engine;
    private final WorkspaceAccess access;
    private final MinioClient storage;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Value("${minio.bucket-name:doc-files}") private String bucket;
    @Value("${app.export.font-path:}") private String fontPath;

    @Transactional
    public Map<String,Object> importFile(MultipartFile file,Long spaceId,int startRow,String columns) throws Exception {
        if(file.getSize()>5*1024*1024 || file.isEmpty())throw new BusinessException(400,"请选择不超过5MB的文件");
        access.requireSpace(spaceId==null?access.currentSpace():spaceId,true);
        String name=Objects.toString(file.getOriginalFilename(),"导入表格.xlsx");
        if(!name.toLowerCase().matches(".*\\.(xlsx|xls|csv)$"))throw new BusinessException(400,"仅支持xlsx、xls、csv");
        if(startRow<1 || startRow>10000)throw new BusinessException(400,"起始行须为1到10000");
        List<Integer> mapping=new ArrayList<>();if(columns!=null&&!columns.isBlank())for(String col:columns.split(","))mapping.add(SheetWorkbookService.coordinate(col.trim().toUpperCase()+"1")[1]);
        try(Workbook book=name.toLowerCase().endsWith(".csv")?csvWorkbook(new String(file.getBytes(),StandardCharsets.UTF_8)):WorkbookFactory.create(file.getInputStream())) {
            if(book.getNumberOfSheets()<1 || book.getNumberOfSheets()>20)throw new BusinessException(400,"工作表数量须为1到20");
            int total=0;
            for(Sheet sheet:book)for(Row row:sheet){total+=row.getPhysicalNumberOfCells();if(row.getRowNum()>=10000 || row.getLastCellNum()>256 || total>20000)throw new BusinessException(400,"导入最多20000个非空单元格、10000行、256列");}
            var req=new DocumentCreateRequest();req.setType(2);req.setTitle(name.replaceFirst("(?i)\\.(xlsx|xls|csv)$",""));req.setSpaceId(spaceId==null?access.currentSpace():spaceId);
            String docId=documents.create(access.userId(),"",req);
            var targets=sheets.selectBySpreadsheetId(docId);SheetSheet first=targets.get(0);
            var formatter=new DataFormatter(Locale.CHINA);var evaluator=book.getCreationHelper().createFormulaEvaluator();
            int rows=0;
            for(int i=0;i<book.getNumberOfSheets();i++) {
                Sheet source=book.getSheetAt(i);SheetSheet target=i==0?first:new SheetSheet();
                target.setSpreadsheetId(docId);target.setSheetName(source.getSheetName());target.setSheetIndex(i);target.setSortOrder(i);target.setRowCount(Math.max(100,source.getLastRowNum()+1));target.setColCount(26);
                if(i==0)sheets.updateById(target);else sheets.insert(target);
                List<CellUpdateRequest> updates=new ArrayList<>();
                for(Row row:source) {
                    if(row.getRowNum()<startRow-1)continue;rows++;
                    List<Integer> chosen=mapping.isEmpty()?java.util.stream.IntStream.range(0,Math.max(0,row.getLastCellNum())).boxed().toList():mapping;
                    for(int out=0;out<chosen.size();out++) {
                        Cell c=row.getCell(chosen.get(out));if(c==null)continue;
                        var value=new CellUpdateRequest();value.setSheetId(target.getId());value.setCellKey(SheetWorkbookService.key(row.getRowNum()-startRow+1,out));
                        value.setContent(c.getCellType()==CellType.FORMULA&&mapping.isEmpty()&&startRow==1?"="+c.getCellFormula():formatter.formatCellValue(c,evaluator));
                        Map<String,Object> f=new LinkedHashMap<>();var style=c.getCellStyle();var font=book.getFontAt(style.getFontIndex());
                        f.put("bl",font.getBold()?1:0);f.put("it",font.getItalic()?1:0);f.put("fs",font.getFontHeightInPoints());f.put("ff",font.getFontName());f.put("align",style.getAlignment().name().toLowerCase());
                        if(c.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(c)){f.put("type","date");value.setContent(c.getLocalDateTimeCellValue().toLocalDate().toString());}
                        if(style instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle xs) {
                            var bg=xs.getFillForegroundColorColor();if(bg!=null&&bg.getARGBHex()!=null)f.put("bg","#"+bg.getARGBHex().substring(2));
                            var color=((org.apache.poi.xssf.usermodel.XSSFFont)font).getXSSFColor();if(color!=null&&color.getARGBHex()!=null)f.put("color","#"+color.getARGBHex().substring(2));
                        }
                        for(var merge:source.getMergedRegions())if(mapping.isEmpty()&&startRow==1&&merge.isInRange(row.getRowNum(),chosen.get(out)))f.put("mc",row.getRowNum()==merge.getFirstRow()&&chosen.get(out)==merge.getFirstColumn()?Map.of("r",merge.getFirstRow(),"c",merge.getFirstColumn(),"rs",merge.getNumberOfCells()/ (merge.getLastColumn()-merge.getFirstColumn()+1),"cs",merge.getLastColumn()-merge.getFirstColumn()+1):Map.of("r",merge.getFirstRow(),"c",merge.getFirstColumn()));
                        value.setFormatJson(JSON.toJSONString(f));updates.add(value);
                    }
                }
                if(!updates.isEmpty()){var batch=new CellBatchUpdateRequest();batch.setSheetId(target.getId());batch.setCells(updates);cells.batchUpdate(access.userId(),batch);}
            }
            jdbc.update("UPDATE sheet_spreadsheet SET sheet_count=? WHERE sheet_id=?",book.getNumberOfSheets(),docId);
            return Map.of("sheetId",docId,"rows",rows);
        }
    }
    public Map<String,Object> export(String docId,String type,Long sheetId,int startRow,String columns) throws Exception {
        access.requireTypedDocument("sheet",docId,false);
        if(!Set.of("xlsx","csv","pdf").contains(type))throw new BusinessException(400,"无效导出格式");
        var data=cells.data(docId);
        if(data.isEmpty())throw new BusinessException(404,"表格不存在");
        List<String> selected=new ArrayList<>(data.keySet());
        if(sheetId!=null){var sheet=sheets.selectById(sheetId);if(sheet==null||!docId.equals(sheet.getSpreadsheetId()))throw new BusinessException(400,"工作表不属于此文档");selected=List.of(sheet.getSheetName());}
        byte[] bytes;
        if(type.equals("xlsx")) {
            try(var book=engine.workbook(data);var out=new ByteArrayOutputStream()) {
                for(var entry:data.entrySet())for(var c:entry.getValue()) {
                    var mc=SheetWorkbookService.format(c.getFormatJson()).getJSONObject("mc");
                    if(mc!=null&&mc.containsKey("rs")&&mc.containsKey("cs")&&(mc.getIntValue("rs")>1||mc.getIntValue("cs")>1))book.getSheet(entry.getKey()).addMergedRegion(new CellRangeAddress(mc.getIntValue("r"),mc.getIntValue("r")+mc.getIntValue("rs")-1,mc.getIntValue("c"),mc.getIntValue("c")+mc.getIntValue("cs")-1));
                }
                book.getCreationHelper().createFormulaEvaluator().evaluateAll();book.write(out);bytes=out.toByteArray();
            }
        } else {
            var values=engine.calculate(data);var grids=new LinkedHashMap<String,List<List<String>>>();
            for(String name:selected) {
                var list=data.get(name);int lastRow=list.stream().mapToInt(SheetCell::getRowIndex).max().orElse(0),lastCol=list.stream().mapToInt(SheetCell::getColIndex).max().orElse(0);
                List<Integer> chosen=new ArrayList<>();if(columns!=null&&!columns.isBlank())for(String col:columns.split(","))chosen.add(SheetWorkbookService.coordinate(col.trim().toUpperCase()+"1")[1]);else for(int col=0;col<=lastCol;col++)chosen.add(col);
                List<List<String>> grid=new ArrayList<>();for(int r=Math.max(0,startRow-1);r<=lastRow;r++){List<String> line=new ArrayList<>();for(int c:chosen)line.add(values.get(name).getOrDefault(SheetWorkbookService.key(r,c),""));grid.add(line);}grids.put(name,grid);
            }
            if(type.equals("csv")){var csv=new StringBuilder("\ufeff");for(var row:grids.values().iterator().next()){csv.append(row.stream().map(SheetTransferService::csvValue).collect(java.util.stream.Collectors.joining(","))).append("\r\n");}bytes=csv.toString().getBytes(StandardCharsets.UTF_8);}
            else bytes=pdf(grids);
        }
        String object="export/"+docId+"-"+UUID.randomUUID()+"."+type;
        String mime=type.equals("xlsx")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":type.equals("pdf")?"application/pdf":"text/csv; charset=utf-8";
        storage.putObject(PutObjectArgs.builder().bucket(bucket).object(object).stream(new ByteArrayInputStream(bytes),bytes.length,-1).contentType(mime).build());
        String url=storage.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(object).method(Method.GET).expiry(1,TimeUnit.HOURS).build());
        return Map.of("downloadUrl",url,"size",bytes.length);
    }
    private byte[] pdf(Map<String,List<List<String>>> grids) throws Exception {
        var out=new ByteArrayOutputStream();var document=new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());com.lowagie.text.pdf.PdfWriter.getInstance(document,out);document.open();
        String path=fontPath;if(path==null||path.isBlank())path=java.nio.file.Files.isRegularFile(java.nio.file.Path.of("C:/Windows/Fonts/simsun.ttc"))?"C:/Windows/Fonts/simsun.ttc,0":"/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0";
        var font=new com.lowagie.text.Font(com.lowagie.text.pdf.BaseFont.createFont(path,com.lowagie.text.pdf.BaseFont.IDENTITY_H,true),10);
        for(var grid:grids.entrySet()) {
            document.add(new com.lowagie.text.Paragraph(grid.getKey(),font));
            int count=grid.getValue().isEmpty()?1:grid.getValue().get(0).size();if(count>20)throw new BusinessException(400,"PDF最多20列，请设置导出列映射");
            var table=new com.lowagie.text.pdf.PdfPTable(Math.max(1,count));table.setWidthPercentage(100);
            for(var row:grid.getValue())for(var value:row)table.addCell(new com.lowagie.text.Phrase(value,font));document.add(table);
        }
        document.close();return out.toByteArray();
    }
    public static String csvValue(String value) {
        // Prevent spreadsheet formula execution when a CSV is opened outside this application.
        if(value.matches("^[=+@].*") || value.startsWith("-")&&!value.matches("-\\d+(?:\\.\\d+)?"))value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    public static Workbook csvWorkbook(String input) {
        var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var sheet=book.createSheet("Sheet1");
        String text=input.startsWith("\ufeff")?input.substring(1):input;var value=new StringBuilder();boolean quoted=false;int row=0,col=0;
        for(int i=0;i<=text.length();i++) {
            if(i==text.length()&&value.isEmpty()&&col==0)break;
            char c=i==text.length()?'\n':text.charAt(i);
            if(c=='"'){if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}
            else if(!quoted&&(c==','||c=='\n'||c=='\r')) {
                if(row>=10000||col>=256)throw new BusinessException(400,"CSV超出行列限制");
                Row target=sheet.getRow(row);if(target==null)target=sheet.createRow(row);target.createCell(col++).setCellValue(value.toString());value.setLength(0);
                if(c!=','){row++;col=0;if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;}
            } else value.append(c);
        }
        if(quoted)throw new BusinessException(400,"CSV引号不完整");return book;
    }
}
