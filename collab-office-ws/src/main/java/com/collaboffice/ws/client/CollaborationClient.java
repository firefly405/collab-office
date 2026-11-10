package com.collaboffice.ws.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 调用 collaboration-service 拉取 Yjs 历史 updates，用于新客户端 join 时的初始化同步。
 */
@FeignClient(name = "collaboration-service", contextId = "collaborationClient")
public interface CollaborationClient {

    @GetMapping("/api/v1/collaboration/{docId}/yjs/updates")
    CommonResult<List<Map<String, Object>>> getYjsUpdates(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable("docId") String docId,
            @RequestParam(value = "afterSeq", defaultValue = "0") long afterSeq,
            @RequestParam(value = "limit", defaultValue = "200") int limit);

    @GetMapping("/api/v1/collaboration/{docId}/yjs/max-seq")
    CommonResult<Long> getYjsMaxSeq(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable("docId") String docId);
}
