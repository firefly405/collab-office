package com.collaboffice.doc.controller;
import com.alibaba.fastjson2.JSONObject;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.doc.service.WhiteboardWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/whiteboard/{board}/operations")
public class WhiteboardWorkspaceController {
 private final WhiteboardWorkspaceService service;
 @GetMapping public CommonResult<List<Map<String,Object>>> list(@PathVariable String board){return CommonResult.success(service.operations(board));}
 @GetMapping("/export-check") public CommonResult<Boolean> export(@PathVariable String board){service.requireExport(board);return CommonResult.success(true);}
 @PostMapping public CommonResult<List<Map<String,Object>>> append(@PathVariable String board,@RequestBody JSONObject body){return CommonResult.success(service.append(board,body));}
 @PutMapping("/{element}/active") public CommonResult<List<Map<String,Object>>> active(@PathVariable String board,@PathVariable Long element,@RequestBody ActiveRequest body){return CommonResult.success(service.active(board,element,body.active()));}
 public record ActiveRequest(boolean active){}
}
