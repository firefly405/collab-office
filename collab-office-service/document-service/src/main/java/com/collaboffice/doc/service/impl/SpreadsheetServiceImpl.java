package com.collaboffice.doc.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.doc.entity.SheetSpreadsheet;
import com.collaboffice.doc.entity.SheetSheet;
import com.collaboffice.doc.mapper.SheetSpreadsheetMapper;
import com.collaboffice.doc.mapper.SheetSheetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpreadsheetServiceImpl {

    private final SheetSpreadsheetMapper sheetSpreadsheetMapper;
    private final SheetSheetMapper sheetSheetMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final com.collaboffice.doc.mapper.SheetCellMapper cells;
    private final com.collaboffice.doc.service.SheetWorkbookService engine;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Transactional
    public String createSpreadsheet(Long userId, String title, Long spaceId) {
        spaceId=access.requireSpace(spaceId==null?access.currentSpace():spaceId,true);
        String sheetId = SnowflakeIdGenerator.nextIdStr();
        SheetSpreadsheet ss = new SheetSpreadsheet();
        ss.setSheetId(sheetId);
        ss.setTitle(title);
        ss.setSpaceId(spaceId);
        ss.setCreatorId(userId);
        ss.setSheetCount(1);
        ss.setRowCount(1000);
        ss.setColCount(26);
        ss.setStatus(1);
        sheetSpreadsheetMapper.insert(ss);

        SheetSheet sheet = new SheetSheet();
        sheet.setSpreadsheetId(ss.getSheetId());
        sheet.setSheetName("Sheet1");
        sheet.setSheetIndex(0);
        sheet.setRowCount(1000);
        sheet.setColCount(26);
        sheet.setSortOrder(0);
        sheetSheetMapper.insert(sheet);
        return sheetId;
    }

    public Map<String, Object> getDetail(String sheetId) {
        SheetSpreadsheet ss = sheetSpreadsheetMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SheetSpreadsheet>()
                        .eq(SheetSpreadsheet::getSheetId, sheetId));
        if (ss == null) return Collections.emptyMap();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sheetId", ss.getSheetId());
        result.put("title", ss.getTitle());
        result.put("sheetCount", ss.getSheetCount());
        result.put("rowCount", ss.getRowCount());
        result.put("colCount", ss.getColCount());
        result.put("creatorId", ss.getCreatorId());
        result.put("status", ss.getStatus());
        List<SheetSheet> sheets = sheetSheetMapper.selectBySpreadsheetId(ss.getSheetId());
        result.put("sheets", sheets);
        return result;
    }

    @Transactional
    public void addSheet(String sheetId, String name) {
        lock(sheetId);
        SheetSpreadsheet ss = findSpreadsheet(sheetId);
        List<SheetSheet> existing = sheetSheetMapper.selectBySpreadsheetId(ss.getSheetId());
        validateName(name,existing,null);
        if(existing.size()>=20)throw new com.collaboffice.common.exception.BusinessException(400,"最多20张工作表");
        SheetSheet sheet = new SheetSheet();
        sheet.setSpreadsheetId(ss.getSheetId());
        sheet.setSheetName(name);
        sheet.setSheetIndex(existing.stream().mapToInt(SheetSheet::getSheetIndex).max().orElse(-1)+1);
        sheet.setRowCount(ss.getRowCount());
        sheet.setColCount(ss.getColCount());
        sheet.setSortOrder(existing.size());
        sheetSheetMapper.insert(sheet);
        ss.setSheetCount(existing.size() + 1);
        sheetSpreadsheetMapper.updateById(ss);
    }

    @Transactional
    public void deleteSheet(String sheetId, int index) {
        lock(sheetId);
        SheetSpreadsheet ss = findSpreadsheet(sheetId);
        List<SheetSheet> sheets = sheetSheetMapper.selectBySpreadsheetId(ss.getSheetId());
        if (index < 0 || index >= sheets.size()) return;
        if (sheets.size() <= 1) return;
        sheetSheetMapper.deleteById(sheets.get(index).getId());
        ss.setSheetCount(sheets.size() - 1);
        sheetSpreadsheetMapper.updateById(ss);
    }

    @Transactional
    public void renameSheet(String sheetId, int index, String name) {
        lock(sheetId);
        SheetSpreadsheet ss = findSpreadsheet(sheetId);
        List<SheetSheet> sheets = sheetSheetMapper.selectBySpreadsheetId(ss.getSheetId());
        if (index < 0 || index >= sheets.size()) return;
        SheetSheet sheet = sheets.get(index);
        validateName(name,sheets,sheet.getId());
        Map<String,List<com.collaboffice.doc.entity.SheetCell>> data=new LinkedHashMap<>();
        for(var item:sheets)data.put(item.getSheetName(),cells.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.collaboffice.doc.entity.SheetCell>().eq(com.collaboffice.doc.entity.SheetCell::getSheetId,item.getId())));
        try(var book=engine.workbook(data)) {
            book.setSheetName(index,name);
            List<com.collaboffice.doc.entity.SheetCell> changed=new ArrayList<>();
            int position=0;for(var entry:data.entrySet()) {
                var target=book.getSheetAt(position++);
                for(var c:entry.getValue())if(c.getFormula()!=null&&!c.getFormula().isBlank()) {
                    var rc=com.collaboffice.doc.service.SheetWorkbookService.coordinate(c.getCellKey());
                    String formula=target.getRow(rc[0]).getCell(rc[1]).getCellFormula();c.setFormula(formula);c.setContent("="+formula);changed.add(c);
                }
            }
            if(!changed.isEmpty())cells.batchInsertOrUpdate(changed);
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        sheet.setSheetName(name);
        sheetSheetMapper.updateById(sheet);
    }

    private void validateName(String name,List<SheetSheet> existing,Long ownId) {
        try{org.apache.poi.ss.util.WorkbookUtil.validateSheetName(name);}catch(Exception e){throw new com.collaboffice.common.exception.BusinessException(400,"工作表名称不合法（1到31字，不能含[]:*?/\\）");}
        if(existing.stream().anyMatch(s->!Objects.equals(s.getId(),ownId)&&s.getSheetName().equalsIgnoreCase(name)))throw new com.collaboffice.common.exception.BusinessException(400,"工作表名称重复");
    }
    private void lock(String sheetId){jdbc.queryForObject("SELECT id FROM sheet_spreadsheet WHERE sheet_id=? FOR UPDATE",Long.class,sheetId);}

    private SheetSpreadsheet findSpreadsheet(String sheetId) {
        SheetSpreadsheet ss = sheetSpreadsheetMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SheetSpreadsheet>()
                        .eq(SheetSpreadsheet::getSheetId, sheetId));
        if (ss == null) throw new RuntimeException("表格不存在: " + sheetId);
        return ss;
    }
}
