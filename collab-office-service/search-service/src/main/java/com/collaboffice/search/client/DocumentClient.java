package com.collaboffice.search.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "document-service", contextId = "documentClient")
public interface DocumentClient {

    @PostMapping("/api/v1/document/internal/search")
    CommonResult<List<Map<String, Object>>> searchInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "scope", required = false) String scope
    );

    @PostMapping("/api/v1/document/internal/suggest")
    CommonResult<List<String>> suggestInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("keyword") String keyword
    );
}
