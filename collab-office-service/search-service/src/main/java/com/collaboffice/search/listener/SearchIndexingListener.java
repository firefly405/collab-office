package com.collaboffice.search.listener;

import com.collaboffice.search.entity.DocumentIndex;
import com.collaboffice.search.entity.TaskIndex;
import com.collaboffice.search.repository.DocumentIndexRepository;
import com.collaboffice.search.repository.TaskIndexRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexingListener {

    private final DocumentIndexRepository documentIndexRepository;
    private final TaskIndexRepository taskIndexRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "search-document-queue")
    public void indexDocument(Map<String, Object> payload) {
        log.info("Received document index update event: {}", payload);
        try {
            DocumentIndex doc = objectMapper.convertValue(payload, DocumentIndex.class);
            if (doc.getId() != null) {
                if ("DELETE".equalsIgnoreCase(String.valueOf(payload.get("action")))) {
                    documentIndexRepository.deleteById(doc.getId());
                    log.info("Deleted document {} from ES index", doc.getId());
                } else {
                    documentIndexRepository.save(doc);
                    log.info("Saved document {} to ES index", doc.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to index document: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "search-task-queue")
    public void indexTask(Map<String, Object> payload) {
        log.info("Received task index update event: {}", payload);
        try {
            TaskIndex task = objectMapper.convertValue(payload, TaskIndex.class);
            if (task.getId() != null) {
                if ("DELETE".equalsIgnoreCase(String.valueOf(payload.get("action")))) {
                    taskIndexRepository.deleteById(task.getId());
                    log.info("Deleted task {} from ES index", task.getId());
                } else {
                    taskIndexRepository.save(task);
                    log.info("Saved task {} to ES index", task.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to index task: {}", e.getMessage());
        }
    }
}
