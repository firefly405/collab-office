package com.collaboffice.notification.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "user-service", contextId = "notificationUserClient")
public interface UserClient {
    @GetMapping("/api/v1/user/internal/{userId}")
    CommonResult<Map<String, Object>> getUserInternal(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable("userId") Long userId
    );
}
