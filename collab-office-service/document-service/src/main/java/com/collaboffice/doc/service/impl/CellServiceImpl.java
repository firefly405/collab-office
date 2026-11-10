package com.collaboffice.doc.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CellServiceImpl implements CellService {
    private final SheetCellMapper cells;
    private final SheetSheetMapper sheets;
    private final WorkspaceAccess access;
    private final SheetWorkbookService engine;
    private final JdbcTemplate jdbc;

    @Override @Transactional
    public void updateCell(Long actor,CellUpdateRequest request) {
        var batch=new CellBatchUpdateRequest();batch.setSheetId(request.getSheetId());batch.setCells(List.of(request));batchUpdate(actor,batch);
    }
    @Override @Transactional
    public void batchUpdate(Long actor,CellBatchUpdateRequest request) {
        access.requireSheet(request.getSheetId(),true);
        if(request.getCells()==null || request.getCells().isEmpty() || request.getCells().size()>10000)throw new BusinessException(400,"单次修改须为1到10000个单元格");
        var sheet=sheets.selectById(request.getSheetId());
        // Serialize workbook writes so formulas see the committed cross-sheet inputs.
        jdbc.queryForObject("SELECT id FROM sheet_spreadsheet WHERE sheet_id=? FOR UPDATE",Long.class,sheet.getSpreadsheetId());
        var existing=cells.selectList(new LambdaQueryWrapper<SheetCell>().eq(SheetCell::getSheetId,sheet.getId()));
        Map<String,SheetCell> byKey=new LinkedHashMap<>();for(var c:existing)byKey.put(c.getCellKey(),c);
        List<SheetCell> changed=new ArrayList<>();
        for(var input:request.getCells()) {
            int[] rc=SheetWorkbookService.coordinate(input.getCellKey());
            if(input.getSheetId()!=null && !Objects.equals(input.getSheetId(),sheet.getId()))throw new BusinessException(400,"批次工作表不一致");
            var cell=byKey.get(input.getCellKey());
            if(Boolean.TRUE.equals(input.getCheckConflict()) && (!Objects.equals(text(cell==null?null:cell.getContent()),text(input.getExpectedContent())) || !SheetWorkbookService.format(cell==null?null:cell.getFormatJson()).equals(SheetWorkbookService.format(input.getExpectedFormatJson()))))
                throw new BusinessException(409,input.getCellKey()+" 已被其他人修改，请重新载入后编辑");
            if(cell==null){cell=new SheetCell();cell.setSheetId(sheet.getId());cell.setCellKey(input.getCellKey());cell.setRowIndex(rc[0]);cell.setColIndex(rc[1]);byKey.put(cell.getCellKey(),cell);}
            if(input.getContent()!=null) {
                cell.setContent(input.getContent());cell.setFormula(input.getContent().startsWith("=")?input.getContent().substring(1):null);
            }
            if(input.getFormula()!=null && !input.getFormula().isBlank()){cell.setFormula(input.getFormula().replaceFirst("^=",""));cell.setContent("="+cell.getFormula());}
            if(input.getFormatJson()!=null){SheetWorkbookService.format(input.getFormatJson());cell.setFormatJson(input.getFormatJson());}
            engine.validate(cell);changed.add(cell);
        }
        persist(changed);
        var data=data(sheet.getSpreadsheetId());var results=engine.calculate(data);
        List<SheetCell> formulas=new ArrayList<>();
        for(var entry:data.entrySet())for(var cell:entry.getValue())if(cell.getFormula()!=null && !cell.getFormula().isBlank()) {
            cell.setResultValue(results.get(entry.getKey()).get(cell.getCellKey()));formulas.add(cell);
        }
        persist(formulas);
    }
    public List<SheetCell> detail(Long sheetId) {
        access.requireSheet(sheetId,false);var sheet=sheets.selectById(sheetId);
        var data=data(sheet.getSpreadsheetId());var result=engine.calculate(data).get(sheet.getSheetName());
        var list=data.get(sheet.getSheetName());for(var cell:list)cell.setResultValue(result.get(cell.getCellKey()));return list;
    }
    public Map<String,List<SheetCell>> data(String spreadsheetId) {
        Map<String,List<SheetCell>> data=new LinkedHashMap<>();
        for(var s:sheets.selectBySpreadsheetId(spreadsheetId)) data.put(s.getSheetName(),cells.selectList(new LambdaQueryWrapper<SheetCell>().eq(SheetCell::getSheetId,s.getId())));
        return data;
    }
    @Override public Map<String,String> getCellsByRange(CellRangeQueryRequest request) {
        String[] parts=request.getRange().split(":");int[] start=SheetWorkbookService.coordinate(parts[0]),end=SheetWorkbookService.coordinate(parts.length>1?parts[1]:parts[0]);
        Map<String,String> result=new LinkedHashMap<>();for(var c:detail(request.getSheetId()))if(c.getRowIndex()>=start[0]&&c.getRowIndex()<=end[0]&&c.getColIndex()>=start[1]&&c.getColIndex()<=end[1])result.put(c.getCellKey(),c.getResultValue());return result;
    }
    private String text(String value){return Objects.toString(value,"");}
    private void persist(List<SheetCell> values){for(int i=0;i<values.size();i+=500)cells.batchInsertOrUpdate(values.subList(i,Math.min(values.size(),i+500)));}
}
