package com.collaboffice.doc.consumer;

import com.collaboffice.doc.service.DocumentContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPersistConsumer {

    private final DocumentContentService contentService;

    @RabbitListener(queues = "doc-persist-queue")
    public void handleDocPersist(String message) {
        log.info("收到文档持久化消息");
        try {
            var json = com.alibaba.fastjson2.JSONObject.parseObject(message);
            String docId = json.getString("docId");
            String content = json.getString("content");
            Long userId = json.getLong("userId");
            if (docId != null && content != null) {
                contentService.saveContent(docId, content, userId != null ? userId : 0L);
                log.debug("文档持久化成功: docId={}", docId);
            } else {
                log.warn("无效的持久化消息，缺少必要字段: {}", message);
            }
        } catch (Exception e) {
            log.error("持久化文档操作失败，消息将被路由到DLQ: {}", message, e);
            throw e; // Re-throw to trigger DLQ routing after retries exhausted
        }
    }
}
