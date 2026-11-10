package com.collaboffice.user.controller;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.service.WorkspaceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/user/workspace")
public class WorkspaceDataController {
    private final WorkspaceDataService data;
    @GetMapping("/statistics") public CommonResult<Map<String,Object>> statistics(@RequestParam(defaultValue="false")boolean personal){return CommonResult.success(data.statistics(personal));}
    @GetMapping("/export") public ResponseEntity<byte[]> export(@RequestParam Set<String> kinds,@RequestParam(defaultValue="false")boolean personal)throws Exception{return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=workspace-data.zip").contentType(MediaType.parseMediaType("application/zip")).body(data.export(kinds,personal));}
}
