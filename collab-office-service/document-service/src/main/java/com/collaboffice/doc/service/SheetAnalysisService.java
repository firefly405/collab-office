package com.collaboffice.doc.service;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.impl.CellServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.formula.*;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import java.util.*;

@Service @RequiredArgsConstructor
public class SheetAnalysisService {
    private final WorkspaceAccess access;
    private final SheetSheetMapper sheets;
    private final CellServiceImpl cells;
    private final SheetWorkbookService engine;
    private final JdbcTemplate jdbc;
    public SheetSheet require(String docId,Long sheetId,boolean write) {
        access.requireTypedDocument("sheet",docId,write);
        var sheet=sheetId==null?sheets.selectBySpreadsheetId(docId).get(0):sheets.selectById(sheetId);
        if(sheet==null||!docId.equals(sheet.getSpreadsheetId()))throw new BusinessException(400,"工作表不属于此文档");return sheet;
    }
    @Transactional
    public void sort(String docId,Long sheetId,Map<String,Object> body) throws Exception {
        var sheet=require(docId,sheetId,true);
        jdbc.queryForObject("SELECT id FROM sheet_spreadsheet WHERE sheet_id=? FOR UPDATE",Long.class,docId);
        var data=cells.detail(sheet.getId());Map<String,SheetCell> byKey=new HashMap<>();for(var c:data)byKey.put(c.getCellKey(),c);
        String range=Objects.toString(body.get("range"),"A2:Z100");String[] parts=range.split(":");if(parts.length!=2)throw new BusinessException(400,"请输入完整排序范围");
        int[] start=SheetWorkbookService.coordinate(parts[0]),end=SheetWorkbookService.coordinate(parts[1]);
        if(end[0]<start[0]||end[1]<start[1]||(end[0]-start[0]+1)*(end[1]-start[1]+1)>10000)throw new BusinessException(400,"排序范围无效或超过10000格");
        for(var c:data){var mc=SheetWorkbookService.format(c.getFormatJson()).getJSONObject("mc");if(mc!=null&&c.getRowIndex()>=start[0]&&c.getRowIndex()<=end[0]&&c.getColIndex()>=start[1]&&c.getColIndex()<=end[1])throw new BusinessException(400,"请先拆分排序范围中的合并单元格");}
        List<Map> rules=body.get("rules")==null?List.of(Map.of("column",Objects.toString(body.get("column"),"A"),"descending",Boolean.TRUE.equals(body.get("descending")))):JSON.parseArray(JSON.toJSONString(body.get("rules")),Map.class);
        if(rules.isEmpty()||rules.size()>5)throw new BusinessException(400,"排序条件须为1到5项");
        for(var rule:rules){int col=SheetWorkbookService.coordinate(rule.get("column").toString()+"1")[1];if(col<start[1]||col>end[1])throw new BusinessException(400,"排序列须位于范围内");}
        List<Integer> rows=new ArrayList<>();for(int r=start[0];r<=end[0];r++)rows.add(r);
        rows.sort((a,b)->{for(var rule:rules){int col=SheetWorkbookService.coordinate(rule.get("column").toString()+"1")[1];var av=byKey.get(SheetWorkbookService.key(a,col));var bv=byKey.get(SheetWorkbookService.key(b,col));String left=av==null?"":Objects.toString(av.getResultValue(),av.getContent()),right=bv==null?"":Objects.toString(bv.getResultValue(),bv.getContent());int cmp=compare(left,right);if(cmp!=0)return Boolean.TRUE.equals(rule.get("descending"))&&!left.isBlank()&&!right.isBlank()?-cmp:cmp;}return 0;});
        List<CellUpdateRequest> updates=new ArrayList<>();
        try(var book=engine.workbook(cells.data(docId))) {
            var eval=XSSFEvaluationWorkbook.create(book);int sheetIndex=book.getSheetIndex(sheet.getSheetName());
            for(int i=0;i<rows.size();i++)for(int col=start[1];col<=end[1];col++) {
                int oldRow=rows.get(i),newRow=start[0]+i;String oldKey=SheetWorkbookService.key(oldRow,col),newKey=SheetWorkbookService.key(newRow,col);var source=byKey.get(oldKey);
                var update=new CellUpdateRequest();update.setSheetId(sheet.getId());update.setCellKey(newKey);update.setContent(source==null?"":Objects.toString(source.getContent(),""));update.setFormatJson(source==null?"{}":Objects.toString(source.getFormatJson(),"{}"));
                if(source!=null&&source.getFormula()!=null&&!source.getFormula().isBlank()) {
                    var tokens=FormulaParser.parse(source.getFormula(),eval,FormulaType.CELL,sheetIndex,oldRow);
                    FormulaShifter.createForRowCopy(sheetIndex,sheet.getSheetName(),oldRow,oldRow,newRow-oldRow,SpreadsheetVersion.EXCEL2007).adjustFormula(tokens,sheetIndex);
                    update.setContent("="+FormulaRenderer.toFormulaString(eval,tokens));
                }
                updates.add(update);
            }
        }
        var batch=new CellBatchUpdateRequest();batch.setSheetId(sheet.getId());batch.setCells(updates);cells.batchUpdate(access.userId(),batch);
    }
    static int compare(String a,String b) {
        if(a.isBlank()||b.isBlank())return a.isBlank()?(b.isBlank()?0:1):-1;
        try{return Double.compare(Double.parseDouble(a),Double.parseDouble(b));}catch(NumberFormatException ignored){return a.compareToIgnoreCase(b);}
    }
    public void settings(String docId,Long sheetId,Map<String,Object> body) {
        var sheet=require(docId,sheetId,true);sheet.setSettingsJson(JSON.toJSONString(Map.of("freezeRow",Boolean.TRUE.equals(body.get("freezeRow")),"freezeColumn",Boolean.TRUE.equals(body.get("freezeColumn")))));sheets.updateById(sheet);
    }
}
