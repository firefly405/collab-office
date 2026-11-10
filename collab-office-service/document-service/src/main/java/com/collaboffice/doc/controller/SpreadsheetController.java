package com.collaboffice.doc.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.doc.service.impl.SpreadsheetServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sheet/manage")
@RequiredArgsConstructor
public class SpreadsheetController {

    private final SpreadsheetServiceImpl spreadsheetService;
    private final com.collaboffice.doc.service.DocumentService documents;

    @PostMapping("/create")
    public CommonResult<String> create(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam String title,
                                       @RequestParam(required = false) Long spaceId) {
        var doc=new com.collaboffice.doc.dto.DocumentCreateRequest();doc.setTitle(title);doc.setSpaceId(spaceId);doc.setType(2);
        return CommonResult.success(documents.create(userId,"",doc));
    }

    @GetMapping("/{sheetId}")
    public CommonResult<Map<String, Object>> detail(@PathVariable String sheetId) {
        return CommonResult.success(spreadsheetService.getDetail(sheetId));
    }

    @PostMapping("/{sheetId}/sheet")
    public CommonResult<Void> addSheet(@PathVariable String sheetId, @RequestParam String name) {
        spreadsheetService.addSheet(sheetId, name);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{sheetId}/sheet/{index}")
    public CommonResult<Void> deleteSheet(@PathVariable String sheetId, @PathVariable int index) {
        spreadsheetService.deleteSheet(sheetId, index);
        return CommonResult.success(null);
    }

    @PutMapping("/{sheetId}/sheet/{index}/rename")
    public CommonResult<Void> renameSheet(@PathVariable String sheetId, @PathVariable int index,
                                          @RequestParam String name) {
        spreadsheetService.renameSheet(sheetId, index, name);
        return CommonResult.success(null);
    }
}
