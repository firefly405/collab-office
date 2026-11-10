package com.collaboffice.search.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", contextId = "userSearchClient")
public interface UserSearchClient {
    @PostMapping("/api/v1/user/internal/search")
    CommonResult<List<Map<String, Object>>> searchInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("keyword") String keyword
    );
}
