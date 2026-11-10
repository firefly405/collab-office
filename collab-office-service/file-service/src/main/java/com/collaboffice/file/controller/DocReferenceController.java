package com.collaboffice.file.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.file.service.DocReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/doc")
@RequiredArgsConstructor
public class DocReferenceController {

    private final DocReferenceService docReferenceService;

    @Operation(summary = "创建文档引用")
    @PostMapping("/{id}/references")
    public CommonResult<Void> createReference(@PathVariable String id,
                                               @RequestHeader("X-User-Id") Long userId,
                                               @RequestParam String targetDocId) {
        docReferenceService.createReference(id, targetDocId, userId);
        return CommonResult.success(null);
    }

    @Operation(summary = "获取文档的出向引用")
    @GetMapping("/{id}/references/outbound")
    public CommonResult<List<Map<String, Object>>> getOutbound(@PathVariable String id) {
        return CommonResult.success(docReferenceService.getOutboundReferences(id));
    }

    @Operation(summary = "获取文档的入向引用(被引用)")
    @GetMapping("/{id}/references/inbound")
    public CommonResult<List<Map<String, Object>>> getInbound(@PathVariable String id) {
        return CommonResult.success(docReferenceService.getInboundReferences(id));
    }

    @Operation(summary = "获取文档引用图谱")
    @GetMapping("/{id}/references/graph")
    public CommonResult<Map<String, Object>> getGraph(@PathVariable String id) {
        return CommonResult.success(docReferenceService.getReferenceGraph(id));
    }
}
