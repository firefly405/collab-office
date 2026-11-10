package com.collaboffice.collab.controller;

import com.collaboffice.collab.service.CollaborationService;
import com.collaboffice.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/collaboration")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collaborationService;

    @GetMapping("/{docId}/state")
    public CommonResult<Map<String, Object>> getDocumentState(@PathVariable String docId) {
        Map<String, Object> state = collaborationService.getDocumentState(docId);
        return CommonResult.success(state);
    }

    @GetMapping("/{docId}/users")
    public CommonResult<Object> getOnlineUsers(@PathVariable String docId) {
        return CommonResult.success(collaborationService.getOnlineUsers(docId));
    }

    @PostMapping("/{docId}/join")
    public CommonResult<Void> joinDocument(@PathVariable String docId, @RequestParam String userId) {
        collaborationService.joinDocument(docId, userId);
        return CommonResult.success(null);
    }

    @PostMapping("/{docId}/leave")
    public CommonResult<Void> leaveDocument(@PathVariable String docId, @RequestParam String userId) {
        collaborationService.leaveDocument(docId, userId);
        return CommonResult.success(null);
    }

    @PostMapping("/{docId}/operation")
    public CommonResult<Void> applyOperation(@PathVariable String docId,
                                              @RequestParam String userId,
                                              @RequestBody Map<String, Object> operation) {
        try {
            String opJson = com.alibaba.fastjson2.JSON.toJSONString(operation);
            collaborationService.applyOperation(docId, userId, opJson);
            return CommonResult.success(null);
        } catch (Exception e) {
            return CommonResult.failed(500, "Failed to apply operation: " + e.getMessage());
        }
    }

    @PostMapping("/{docId}/persist")
    public CommonResult<Void> persistDocument(@PathVariable String docId) {
        collaborationService.persistDocumentState(docId);
        return CommonResult.success(null);
    }

    /**
     * 拉取文档历史 Yjs updates，用于新客户端 join 时的初始化同步。
     * GET /api/v1/collaboration/{docId}/yjs/updates?afterSeq=0&limit=500
     */
    @GetMapping("/{docId}/yjs/updates")
    public CommonResult<List<Map<String, Object>>> getYjsUpdates(@PathVariable String docId,
                                                                  @RequestParam(defaultValue = "0") long afterSeq,
                                                                  @RequestParam(defaultValue = "500") int limit) {
        return CommonResult.success(collaborationService.getYjsUpdates(docId, afterSeq, limit));
    }

    /**
     * 查询文档当前最大 seq。
     * GET /api/v1/collaboration/{docId}/yjs/max-seq
     */
    @GetMapping("/{docId}/yjs/max-seq")
    public CommonResult<Long> getYjsMaxSeq(@PathVariable String docId) {
        return CommonResult.success(collaborationService.getYjsMaxSeq(docId));
    }
}
