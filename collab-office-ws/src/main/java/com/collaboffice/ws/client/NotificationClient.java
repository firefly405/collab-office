package com.collaboffice.ws.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "notification-service", contextId = "notificationClient")
public interface NotificationClient {
    @GetMapping("/api/v1/notification/internal/offline")
    CommonResult<List<Map<String, Object>>> getOfflineNotifications(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("userId") Long userId,
            @RequestParam("lastSeqId") Long lastSeqId
    );
}
