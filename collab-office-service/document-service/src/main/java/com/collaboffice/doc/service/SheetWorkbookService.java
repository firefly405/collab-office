package com.collaboffice.doc.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.doc.entity.SheetCell;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import java.util.*;

/** The installed POI engine is shared by calculation, import and export. */
@Service
public class SheetWorkbookService {
    static { org.apache.poi.ss.formula.WorkbookEvaluator.registerFunction("DATEDIF",(org.apache.poi.ss.formula.functions.Function)SheetWorkbookService::dateDifference); }
    public static int[] coordinate(String key) {
        if(key==null || !key.matches("[A-Z]{1,3}[1-9][0-9]{0,4}")) throw new BusinessException(400,"无效单元格坐标");
        var ref=new CellReference(key);
        if(ref.getRow()>=10000 || ref.getCol()>=256) throw new BusinessException(400,"本地版支持10000行、256列");
        return new int[]{ref.getRow(),ref.getCol()};
    }
    public static String key(int row,int col) { return CellReference.convertNumToColString(col)+(row+1); }
    public static JSONObject format(String value) {
        if(value==null || value.isBlank()) return new JSONObject();
        try {
            if(value.length()>10000) throw new IllegalArgumentException();
            var result=JSON.parseObject(value);if(result==null)throw new IllegalArgumentException();return result;
        } catch(Exception e) { throw new BusinessException(400,"无效的单元格格式"); }
    }
    public void validate(SheetCell cell) {
        var f=format(cell.getFormatJson());
        String value=Objects.toString(cell.getContent(),"");
        var merge=f.getJSONObject("mc");if(merge!=null){int row=merge.getIntValue("r"),col=merge.getIntValue("c");if(row<0||col<0||row>=10000||col>=256)throw new BusinessException(400,"合并单元格坐标无效");if(merge.containsKey("rs")&&(merge.getIntValue("rs")<1||merge.getIntValue("cs")<1||row+merge.getIntValue("rs")>10000||col+merge.getIntValue("cs")>256))throw new BusinessException(400,"合并单元格范围无效");}
        if(value.length()>32000) throw new BusinessException(400,"单元格内容过长");
        if(value.isBlank() || value.startsWith("=")) return;
        try {
            switch(f.getString("type")==null?"text":f.getString("type")) {
                case "number" -> {double n=Double.parseDouble(value); if(!Double.isFinite(n)) throw new IllegalArgumentException();}
                case "date" -> java.time.LocalDate.parse(value);
                case "dropdown" -> {if(!Arrays.asList(f.getString("options").split(",")).contains(value)) throw new IllegalArgumentException();}
            }
            if(f.containsKey("min") && Double.parseDouble(value)<f.getDoubleValue("min")) throw new IllegalArgumentException();
            if(f.containsKey("max") && Double.parseDouble(value)>f.getDoubleValue("max")) throw new IllegalArgumentException();
        } catch(Exception e) { throw new BusinessException(400,cell.getCellKey()+" 不符合数据类型或验证规则"); }
    }
    public XSSFWorkbook workbook(Map<String,List<SheetCell>> data) {
        var book=new XSSFWorkbook();
        for(String name:data.keySet()) book.createSheet(name);
        for(var entry:data.entrySet()) {
            Sheet sheet=book.getSheet(entry.getKey());
            for(var source:entry.getValue()) {
                int[] rc=coordinate(source.getCellKey());
                Row row=sheet.getRow(rc[0]); if(row==null) row=sheet.createRow(rc[0]);
                Cell cell=row.createCell(rc[1]);
                String raw=Objects.toString(source.getContent(),"");
                String formula=source.getFormula();
                if((formula==null || formula.isBlank()) && raw.startsWith("=")) formula=raw.substring(1);
                if(formula!=null && !formula.isBlank()) {
                    try {cell.setCellFormula(formula.startsWith("=")?formula.substring(1):formula);}
                    catch(Exception e){cell.setCellErrorValue(FormulaError.NAME.getCode());}
                } else {
                    var f=format(source.getFormatJson());
                    if("date".equals(f.getString("type")) && !raw.isBlank()) {
                        try {cell.setCellValue(java.sql.Date.valueOf(raw));} catch(Exception e){cell.setCellValue(raw);}
                    } else if(!"text".equals(f.getString("type")) && raw.matches("-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?")) {
                        cell.setCellValue(Double.parseDouble(raw));
                    } else if("TRUE".equalsIgnoreCase(raw) || "FALSE".equalsIgnoreCase(raw)) cell.setCellValue(Boolean.parseBoolean(raw));
                    else cell.setCellValue(raw);
                }
                applyFormat(book,cell,format(source.getFormatJson()));
            }
        }
        return book;
    }
    public Map<String,Map<String,String>> calculate(Map<String,List<SheetCell>> data) {
        try(var book=workbook(data)) {
            var evaluator=book.getCreationHelper().createFormulaEvaluator();
            var formatter=new DataFormatter(Locale.CHINA);
            Map<String,Map<String,String>> result=new LinkedHashMap<>();
            for(var entry:data.entrySet()) {
                Map<String,String> values=new LinkedHashMap<>();result.put(entry.getKey(),values);
                for(var source:entry.getValue()) {
                    int[] rc=coordinate(source.getCellKey());var cell=book.getSheet(entry.getKey()).getRow(rc[0]).getCell(rc[1]);
                    try {values.put(source.getCellKey(),formatter.formatCellValue(cell,evaluator));}
                    catch(Exception e) {values.put(source.getCellKey(),"#NAME?");}
                }
            }
            return result;
        } catch(java.io.IOException e) {throw new IllegalStateException(e);}
    }
    private void applyFormat(XSSFWorkbook book,Cell cell,JSONObject f) {
        var style=book.createCellStyle();var font=book.createFont();
        font.setBold(f.getIntValue("bl")==1);font.setItalic(f.getIntValue("it")==1);
        font.setFontHeightInPoints((short)Math.max(8,Math.min(48,f.getIntValue("fs",13))));
        if(f.getString("ff")!=null) font.setFontName(f.getString("ff"));
        if(color(f.getString("color"))!=null)font.setColor(color(f.getString("color")));
        style.setFont(font);
        if(color(f.getString("bg"))!=null){style.setFillForegroundColor(color(f.getString("bg")));style.setFillPattern(FillPatternType.SOLID_FOREGROUND);}
        if(f.getBooleanValue("border")){style.setBorderTop(BorderStyle.THIN);style.setBorderBottom(BorderStyle.THIN);style.setBorderLeft(BorderStyle.THIN);style.setBorderRight(BorderStyle.THIN);}
        try{style.setAlignment(HorizontalAlignment.valueOf(Objects.toString(f.get("align"),"left").toUpperCase()));}catch(Exception ignored){}
        if("date".equals(f.getString("type")))style.setDataFormat(book.createDataFormat().getFormat("yyyy-mm-dd"));
        cell.setCellStyle(style);
    }
    private XSSFColor color(String color) {
        if(color==null || !color.matches("#[0-9a-fA-F]{6}"))return null;
        return new XSSFColor(java.awt.Color.decode(color),null);
    }
    private static org.apache.poi.ss.formula.eval.ValueEval dateDifference(org.apache.poi.ss.formula.eval.ValueEval[] args,int row,int col) {
        if(args.length!=3)return org.apache.poi.ss.formula.eval.ErrorEval.VALUE_INVALID;
        try {
            var start=DateUtil.getLocalDateTime(org.apache.poi.ss.formula.eval.OperandResolver.coerceValueToDouble(org.apache.poi.ss.formula.eval.OperandResolver.getSingleValue(args[0],row,col))).toLocalDate();
            var end=DateUtil.getLocalDateTime(org.apache.poi.ss.formula.eval.OperandResolver.coerceValueToDouble(org.apache.poi.ss.formula.eval.OperandResolver.getSingleValue(args[1],row,col))).toLocalDate();
            String unit=org.apache.poi.ss.formula.eval.OperandResolver.coerceValueToString(org.apache.poi.ss.formula.eval.OperandResolver.getSingleValue(args[2],row,col)).toUpperCase();
            if(end.isBefore(start))return org.apache.poi.ss.formula.eval.ErrorEval.NUM_ERROR;
            long years=java.time.temporal.ChronoUnit.YEARS.between(start,end),months=java.time.temporal.ChronoUnit.MONTHS.between(start,end);
            long value=switch(unit){case "Y"->years;case "M"->months;case "D"->java.time.temporal.ChronoUnit.DAYS.between(start,end);case "YM"->months%12;case "YD"->java.time.temporal.ChronoUnit.DAYS.between(start.plusYears(years),end);case "MD"->java.time.temporal.ChronoUnit.DAYS.between(start.plusMonths(months),end);default->-1;};
            return value<0?org.apache.poi.ss.formula.eval.ErrorEval.NUM_ERROR:new org.apache.poi.ss.formula.eval.NumberEval(value);
        }catch(Exception e){return org.apache.poi.ss.formula.eval.ErrorEval.VALUE_INVALID;}
    }
}
