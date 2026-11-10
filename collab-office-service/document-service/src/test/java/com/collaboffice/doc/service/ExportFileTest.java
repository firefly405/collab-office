package com.collaboffice.doc.service;

import com.collaboffice.doc.service.impl.ExportServiceImpl;
import org.junit.jupiter.api.Test;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class ExportFileTest {
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="prd.export.directory",matches=".+")
    void downloadedApiArtifactsCanBeReopened() throws Exception {
        var directory=java.nio.file.Path.of(System.getProperty("prd.export.directory"));
        try(var word=new XWPFDocument(java.nio.file.Files.newInputStream(directory.resolve("prd-batch2-export.docx")))) {
            assertTrue(word.getParagraphs().stream().anyMatch(p->p.getText().contains("PRD 内容复制验收")));
            assertTrue(word.getParagraphs().stream().anyMatch(p->p.getText().contains("中文与正文必须保留")));
        }
        var pdf=new com.lowagie.text.pdf.PdfReader(java.nio.file.Files.readAllBytes(directory.resolve("prd-batch2-export.pdf")));
        try {
            assertTrue(pdf.getNumberOfPages()>0);
            assertTrue(new com.lowagie.text.pdf.parser.PdfTextExtractor(pdf).getTextFromPage(1).contains("中文与正文必须保留"));
        } finally {pdf.close();}
    }
    byte[] export(String method) throws Exception {
        var service=new ExportServiceImpl(null,null,null,null,null);
        var conversion=ExportServiceImpl.class.getDeclaredMethod(method,String.class);conversion.setAccessible(true);
        return (byte[])conversion.invoke(service,DocumentExportContent.normalize("<h1>PRD Export</h1><p>验收正文 &amp; content</p>"));
    }
    @Test void exportOptionsIncludeActualCommentAndVersionText() throws Exception {
        var jdbc=org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        org.mockito.Mockito.when(jdbc.queryForList(org.mockito.ArgumentMatchers.contains("FROM doc_comment"),org.mockito.ArgumentMatchers.eq((Object)"10"))).thenReturn(java.util.List.of(java.util.Map.of("id",1,"user_name","Alice","content","Review text","is_resolved",0)));
        org.mockito.Mockito.when(jdbc.queryForList(org.mockito.ArgumentMatchers.contains("FROM doc_content c"),org.mockito.ArgumentMatchers.eq((Object)"10"))).thenReturn(java.util.List.of(java.util.Map.of("version",1,"editor_name","Bob","edit_time","2026-10-15","content","<p>Previous body</p>")));
        var service=new ExportServiceImpl(null,null,new com.fasterxml.jackson.databind.ObjectMapper(),null,jdbc);
        var append=ExportServiceImpl.class.getDeclaredMethod("withAppendices",String.class,String.class,String.class);append.setAccessible(true);
        String result=(String)append.invoke(service,"10",DocumentExportContent.normalize("<p>Current body</p>"),"comments versions");
        assertTrue(result.contains("Review text"));assertTrue(result.contains("Previous body"));assertTrue(result.contains("Current body"));
    }
    @Test void wordFileCanBeReopened() throws Exception {
        try(var file=new XWPFDocument(new ByteArrayInputStream(export("toWord")))) {
            assertEquals("PRD Export",file.getParagraphs().get(0).getText());
            assertTrue(file.getParagraphs().stream().anyMatch(p->p.getText().contains("验收正文 & content")));
        }
    }
    @Test void pdfFileCanBeReopened() throws Exception {
        var pdf=new com.lowagie.text.pdf.PdfReader(export("toPdf"));
        try {assertTrue(pdf.getNumberOfPages()>0);assertTrue(new com.lowagie.text.pdf.parser.PdfTextExtractor(pdf).getTextFromPage(1).contains("PRD Export"));}
        finally {pdf.close();}
    }
}
