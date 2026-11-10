package com.collaboffice.doc.controller;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.SheetChartMapper;
import com.collaboffice.doc.service.*;
import com.collaboffice.doc.service.impl.CellServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/api/v1/sheet") @RequiredArgsConstructor
public class SheetController {
    private final CellServiceImpl cells;
    private final SheetTransferService transfer;
    private final SheetAnalysisService analysis;
    private final SheetChartMapper charts;
    private final DocumentService documents;
    private final WorkspaceAccess access;
    @PostMapping("/create") public CommonResult<String> create(@RequestHeader("X-User-Id")Long actor,@Valid @RequestBody SheetCreateRequest request) {
        var doc=new DocumentCreateRequest();doc.setTitle(request.getTitle());doc.setSpaceId(request.getSpaceId());doc.setType(2);return CommonResult.success(documents.create(actor,"",doc));
    }
    @PutMapping("/cell") public CommonResult<Void> cell(@RequestHeader("X-User-Id")Long actor,@Valid @RequestBody CellUpdateRequest body){cells.updateCell(actor,body);return CommonResult.success(null);}
    @PutMapping("/cell/batch") public CommonResult<Void> batch(@RequestHeader("X-User-Id")Long actor,@Valid @RequestBody CellBatchUpdateRequest body){cells.batchUpdate(actor,body);return CommonResult.success(null);}
    @GetMapping("/cell/range") public CommonResult<Map<String,String>> range(@Valid CellRangeQueryRequest body){return CommonResult.success(cells.getCellsByRange(body));}
    @GetMapping("/cell/details") public CommonResult<List<SheetCell>> details(@RequestParam Long sheetId){return CommonResult.success(cells.detail(sheetId));}
    @PostMapping("/import") public CommonResult<Map<String,Object>> importFile(@RequestParam MultipartFile file,@RequestParam(required=false)Long spaceId,@RequestParam(defaultValue="1")int startRow,@RequestParam(required=false)String columns) throws Exception{return CommonResult.success(transfer.importFile(file,spaceId,startRow,columns));}
    @GetMapping("/{docId}/export") public CommonResult<Map<String,Object>> export(@PathVariable String docId,@RequestParam(defaultValue="xlsx")String format,@RequestParam(required=false)Long sheetId,@RequestParam(defaultValue="1")int startRow,@RequestParam(required=false)String columns) throws Exception{return CommonResult.success(transfer.export(docId,format,sheetId,startRow,columns));}
    @PostMapping("/{docId}/sort") public CommonResult<Void> sort(@PathVariable String docId,@RequestBody Map<String,Object> body) throws Exception{analysis.sort(docId,child(body),body);return CommonResult.success(null);}
    @PutMapping("/{docId}/settings") public CommonResult<Void> settings(@PathVariable String docId,@RequestBody Map<String,Object> body){analysis.settings(docId,child(body),body);return CommonResult.success(null);}
    private Long child(Map<String,Object> body){return body.get("sheetId")==null?null:Long.valueOf(body.get("sheetId").toString());}

    @GetMapping("/{docId}/charts") public CommonResult<List<SheetChart>> charts(@PathVariable String docId){return CommonResult.success(charts.selectBySheetId(docId));}
    @PostMapping("/{docId}/charts") public CommonResult<SheetChart> createChart(@PathVariable String docId,@RequestBody SheetChart body){validateChart(docId,body);body.setId(null);body.setSheetId(docId);body.setCreatedAt(java.time.LocalDateTime.now());body.setUpdatedAt(java.time.LocalDateTime.now());charts.insert(body);return CommonResult.success(body);}
    @PutMapping("/{docId}/charts/{chartId}") public CommonResult<SheetChart> updateChart(@PathVariable String docId,@PathVariable Long chartId,@RequestBody SheetChart body){var existing=charts.selectById(chartId);if(existing==null||!docId.equals(existing.getSheetId()))throw new BusinessException(404,"图表不存在");validateChart(docId,body);body.setId(chartId);body.setSheetId(docId);body.setUpdatedAt(java.time.LocalDateTime.now());charts.updateById(body);return CommonResult.success(body);}
    @DeleteMapping("/{docId}/charts/{chartId}") public CommonResult<Void> deleteChart(@PathVariable String docId,@PathVariable Long chartId){charts.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SheetChart>().eq(SheetChart::getSheetId,docId).eq(SheetChart::getId,chartId));return CommonResult.success(null);}
    private void validateChart(String docId,SheetChart chart) {
        if(!Set.of("bar","line","pie","scatter").contains(Objects.toString(chart.getChartType(),"")))throw new BusinessException(400,"不支持的图表类型");
        try{var config=JSON.parseObject(chart.getConfigJson());analysis.require(docId,config.getLong("sheetId"),true);for(String key:List.of("labels","values")){String[] range=config.getString(key).split(":");if(range.length!=2)throw new IllegalArgumentException();SheetWorkbookService.coordinate(range[0]);SheetWorkbookService.coordinate(range[1]);}}
        catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(400,"图表范围无效");}
    }
    @PostMapping("/{docId}/filter") public CommonResult<List<Map<String,Object>>> filter(@PathVariable String docId,@RequestBody Map<String,Object> body) {
        var sheet=analysis.require(docId,child(body),false);int col=SheetWorkbookService.coordinate(Objects.toString(body.get("column"),"A")+"1")[1];String expected=Objects.toString(body.get("value"),"");var data=cells.detail(sheet.getId());Set<Integer> rows=new HashSet<>();
        for(var c:data)if(c.getColIndex()==col){String value=Objects.toString(c.getResultValue(),"");boolean matches=switch(Objects.toString(body.get("operator"),"contains")){case "equals"->value.equals(expected);case "starts_with"->value.startsWith(expected);default->value.contains(expected);};if(matches)rows.add(c.getRowIndex());}
        return CommonResult.success(data.stream().filter(c->rows.contains(c.getRowIndex())).map(c->Map.<String,Object>of("cellKey",c.getCellKey(),"value",Objects.toString(c.getResultValue(),""))).toList());
    }
}
