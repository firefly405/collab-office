package com.collaboffice.task.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@FeignClient(name = "notification-service", contextId = "notificationClient")
public interface NotificationClient {
    @PostMapping("/api/v1/notification/internal/send")
    CommonResult<Void> sendNotificationInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestBody Map<String, Object> params
    );
}
