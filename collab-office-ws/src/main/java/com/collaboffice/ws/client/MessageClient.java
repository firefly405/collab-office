package com.collaboffice.ws.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "message-service", contextId = "messageClient")
public interface MessageClient {
    @GetMapping("/api/v1/chat/internal/offline")
    CommonResult<List<Map<String, Object>>> getOfflineMessages(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("userId") Long userId,
            @RequestParam("lastSeqId") Long lastSeqId
    );
}
