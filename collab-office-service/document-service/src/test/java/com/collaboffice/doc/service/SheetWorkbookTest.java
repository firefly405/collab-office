package com.collaboffice.doc.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.doc.entity.SheetCell;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SheetWorkbookTest {
    private final SheetWorkbookService engine=new SheetWorkbookService();
    private SheetCell cell(String key,String value){var c=new SheetCell();c.setCellKey(key);c.setContent(value);return c;}
    @Test void formulasCrossSheetsErrorsAndUpdatesUsePoi() {
        var input=List.of(cell("A1","10"),cell("A2","20"),cell("B1","=SUM(A1:A2)"),cell("B2","=AVERAGE(A1:A2)"),cell("B3","=IF(AND(A1>0,A2>0),MAX(A1:A2),0)"),cell("B4","=LEFT(\"ABC\",2)"),cell("B5","=INDEX(A1:A2,MATCH(20,A1:A2,0))"),cell("B6","=VLOOKUP(10,A1:B2,2,FALSE)"),cell("B7","=1/0"));
        Map<String,List<SheetCell>> data=new LinkedHashMap<>();data.put("预算",input);data.put("汇总",List.of(cell("A1","='预算'!B1*2")));
        var result=engine.calculate(data);assertEquals("30",result.get("预算").get("B1"));assertEquals("15",result.get("预算").get("B2"));assertEquals("20",result.get("预算").get("B3"));assertEquals("AB",result.get("预算").get("B4"));assertEquals("20",result.get("预算").get("B5"));assertEquals("30",result.get("预算").get("B6"));assertEquals("#DIV/0!",result.get("预算").get("B7"));assertEquals("60",result.get("汇总").get("A1"));
        input.get(0).setContent("40");assertEquals("120",engine.calculate(data).get("汇总").get("A1"));
    }
    @Test void requiredTextDateAndLookupFunctions() {
        String[][] samples={{"=CONCAT(\"A\",\"B\")","AB"},{"=DATEDIF(DATE(2024,1,1),DATE(2026,1,1),\"Y\")","2"},{"=HLOOKUP(10,A1:B2,2,FALSE)","20"},{"=RIGHT(\"abc\",2)","bc"},{"=MID(\"abc\",2,1)","b"},{"=IF(OR(NOT(FALSE),FALSE),COUNT(A1:A2),0)","2"},{"=MIN(A1:A2)","10"}};
        var data=new ArrayList<>(List.of(cell("A1","10"),cell("A2","20")));for(int i=0;i<samples.length;i++)data.add(cell("D"+(i+1),samples[i][0]));
        var result=engine.calculate(Map.of("Sheet1",data)).get("Sheet1");for(int i=0;i<samples.length;i++)assertEquals(samples[i][1],result.get("D"+(i+1)),samples[i][0]);
        var date=cell("E1","=TODAY()");date.setFormatJson("{\"type\":\"date\"}");assertTrue(engine.calculate(Map.of("Sheet1",List.of(date))).get("Sheet1").get("E1").matches("\\d{4}-\\d{2}-\\d{2}"));
    }
    @Test void typedValidationAndCoordinatesRejectInvalidInputs() {
        var c=cell("A1","abc");c.setFormatJson("{\"type\":\"number\",\"min\":0}");assertThrows(BusinessException.class,()->engine.validate(c));c.setContent("-1");assertThrows(BusinessException.class,()->engine.validate(c));c.setContent("3");assertDoesNotThrow(()->engine.validate(c));
        c.setFormatJson("{\"type\":\"dropdown\",\"options\":\"待办,完成\"}");assertThrows(BusinessException.class,()->engine.validate(c));c.setContent("完成");assertDoesNotThrow(()->engine.validate(c));
        for(String key:List.of("A0","A10001","ZZZ1","not a cell"))assertThrows(BusinessException.class,()->SheetWorkbookService.coordinate(key));
    }
    @Test void csvQuotesCommasNewlinesAndFormulaInjection() throws Exception {
        try(var book=SheetTransferService.csvWorkbook("\ufeff名称,说明\r\n\"项目,A\",\"第一行\n第二行 \"\"引用\"\"\"")) {
            assertEquals("项目,A",book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());assertEquals("第一行\n第二行 \"引用\"",book.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
        assertEquals("\"'=SUM(A1)\"",SheetTransferService.csvValue("=SUM(A1)"));assertThrows(BusinessException.class,()->SheetTransferService.csvWorkbook("\"bad"));
    }
    @Test void workbookStyleAndCrossSheetRenamePreserveFormula() throws Exception {
        var c=cell("A1","=Sheet2!A1");c.setFormatJson("{\"bl\":1,\"bg\":\"#3370ff\",\"align\":\"center\"}");var data=new LinkedHashMap<String,List<SheetCell>>();data.put("Sheet1",List.of(c));data.put("Sheet2",List.of(cell("A1","3")));
        try(var book=engine.workbook(data)){book.setSheetName(1,"费用");var result=book.getSheetAt(0).getRow(0).getCell(0);assertEquals("费用!A1",result.getCellFormula());assertEquals(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER,result.getCellStyle().getAlignment());assertTrue(book.getFontAt(result.getCellStyle().getFontIndex()).getBold());}
    }
    @Test void excelFormatsReopenAndProduceLocalImportFixtures() throws Exception {
        String directory=System.getProperty("prd.export.directory");
        for(String type:List.of("xls","xlsx")) {
            try(org.apache.poi.ss.usermodel.Workbook book=type.equals("xls")?new org.apache.poi.hssf.usermodel.HSSFWorkbook():new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                var sheet=book.createSheet("预算");sheet.createRow(0).createCell(0).setCellValue("中文预算");sheet.getRow(0).createCell(1).setCellValue(12);sheet.getRow(0).createCell(2).setCellFormula("B1*2");book.getCreationHelper().createFormulaEvaluator().evaluateAll();
                var out=new java.io.ByteArrayOutputStream();book.write(out);
                try(var reopened=org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(out.toByteArray()))){assertEquals("中文预算",reopened.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());assertEquals(24,reopened.getSheetAt(0).getRow(0).getCell(2).getNumericCellValue());}
                if(directory!=null)java.nio.file.Files.write(java.nio.file.Path.of(directory,"prd-batch5-import."+type),out.toByteArray());
            }
        }
        if(directory!=null&&java.nio.file.Files.exists(java.nio.file.Path.of(directory,"prd-batch5-export.xlsx"))) {
            try(var book=org.apache.poi.ss.usermodel.WorkbookFactory.create(java.nio.file.Files.newInputStream(java.nio.file.Path.of(directory,"prd-batch5-export.xlsx")))){assertEquals("预算",book.getSheetAt(0).getSheetName());assertEquals("C2*2",book.getSheetAt(0).getRow(1).getCell(3).getCellFormula());assertTrue(book.getSheetAt(0).getNumMergedRegions()>0);}
            try(var reader=new com.lowagie.text.pdf.PdfReader(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(directory,"prd-batch5-export.pdf")))){assertTrue(new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1).contains("预算"));}
        }
    }
}
