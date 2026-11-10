package com.collaboffice.search.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "task-service", contextId = "taskSearchClient")
public interface TaskSearchClient {
    @PostMapping("/api/v1/task/internal/search")
    CommonResult<List<Map<String, Object>>> searchInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("keyword") String keyword
    );
}
