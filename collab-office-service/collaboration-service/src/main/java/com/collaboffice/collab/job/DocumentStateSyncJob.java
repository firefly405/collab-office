package com.collaboffice.collab.job;

import com.collaboffice.collab.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStateSyncJob {

    private final CollaborationService collaborationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USERS_PREFIX = "doc:users:";

    @Scheduled(fixedRate = 300000)
    public void syncDocumentStates() {
        log.debug("Scheduled document state sync - checking active documents...");
        Set<String> activeDocKeys = scanKeys(USERS_PREFIX + "*");
        if (activeDocKeys == null || activeDocKeys.isEmpty()) {
            log.debug("No active documents to sync");
            return;
        }
        for (String key : activeDocKeys) {
            String docId = key.replace(USERS_PREFIX, "");
            try {
                collaborationService.persistDocumentState(docId);
            } catch (Exception e) {
                log.error("Failed to persist state for docId={}: {}", docId, e.getMessage());
            }
        }
        log.info("Document state sync completed for {} active documents", activeDocKeys.size());
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
        } catch (Exception e) {
            log.warn("SCAN operation failed, falling back to KEYS: {}", e.getMessage());
            Set<String> fallback = redisTemplate.keys(pattern);
            if (fallback != null) keys.addAll(fallback);
        }
        return keys;
    }
}
