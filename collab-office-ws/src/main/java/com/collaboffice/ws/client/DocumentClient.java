package com.collaboffice.ws.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调用 document-service 校验用户对文档的访问权限，供 DOC_JOIN 时使用。
 */
@FeignClient(name = "document-service", contextId = "documentClient")
public interface DocumentClient {

    /**
     * 校验用户是否有权访问文档(供 ws-service 在 DOC_JOIN 时调用)。
     * 通过条件: 文档创建者 / 文档成员 / 公开文档
     */
    @GetMapping("/api/v1/document/internal/permission/check")
    CommonResult<Boolean> checkDocPermission(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("docId") String docId,
            @RequestParam("userId") Long userId,
            @RequestParam("write") boolean write
    );
}
