package com.collaboffice.doc.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.doc.entity.DocContent;
import com.collaboffice.doc.entity.DocDocument;
import com.collaboffice.doc.mapper.DocContentMapper;
import com.collaboffice.doc.mapper.DocDocumentMapper;
import com.collaboffice.doc.service.DocumentContentService;
import com.collaboffice.doc.vo.DocumentContentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentContentServiceImpl implements DocumentContentService {

    private final DocContentMapper docContentMapper;
    private final DocDocumentMapper docDocumentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    private static final String CACHE_PREFIX = "cache:doc:content:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Override
    public DocumentContentVO getContent(String docId) {
        String cacheKey = CACHE_PREFIX + docId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof DocumentContentVO vo) {
            vo.setContent(com.collaboffice.doc.service.DocumentExportContent.unwrapHtml(vo.getContent()));
            return vo;
        }
        if (cached != null) {
            redisTemplate.delete(cacheKey);
        }

        DocContent latest = docContentMapper.selectLatestByDocId(docId);
        if (latest == null) throw new BusinessException(ResultCode.NOT_FOUND);

        DocDocument doc = docDocumentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                        .eq(DocDocument::getDocId, docId));

        DocumentContentVO vo = new DocumentContentVO();
        vo.setDocId(docId);
        vo.setTitle(doc != null ? doc.getTitle() : "");
        vo.setContent(com.collaboffice.doc.service.DocumentExportContent.unwrapHtml(latest.getContent()));
        vo.setVersion(latest.getVersion());
        vo.setLastEditor(latest.getEditorName());
        vo.setLastEditTime(latest.getEditTime());

        redisTemplate.opsForValue().set(cacheKey, vo, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return vo;
    }

    @Override
    public void saveContent(String docId, String content, Long userId) {
        content=com.collaboffice.doc.service.DocumentExportContent.unwrapHtml(content);
        String lockKey = "lock:doc:save:" + docId;
        boolean acquired = false;
        int retries = 30;
        while (retries > 0) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                acquired = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "保存操作被中断");
            }
            retries--;
        }
        if (!acquired) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "保存文档冲突，请重试");
        }

        try {
            DocDocument doc = docDocumentMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                            .eq(DocDocument::getDocId, docId));
            if (doc == null) throw new BusinessException(ResultCode.NOT_FOUND);

            long newVersion = doc.getCurrentVersion() + 1;
            String contentHash = sha256(content);

            DocContent newContent = new DocContent();
            newContent.setDocId(docId);
            newContent.setVersion(newVersion);
            newContent.setContent(content);
            newContent.setContentHash(contentHash);
            var lastSnapshot=jdbc.queryForList("SELECT edit_time FROM doc_content WHERE doc_id=? AND is_snapshot=1 ORDER BY version DESC LIMIT 1",java.sql.Timestamp.class,docId);
            var previous=docContentMapper.selectLatestByDocId(docId);
            boolean snapshot=lastSnapshot.isEmpty()||lastSnapshot.get(0)==null||lastSnapshot.get(0).toLocalDateTime().isBefore(LocalDateTime.now().minusMinutes(5))
                    ||previous!=null&&Math.abs(content.length()-java.util.Objects.toString(previous.getContent(),"").length())>=500;
            newContent.setIsSnapshot(snapshot?1:0);
            if(snapshot)newContent.setRemark("自动快照");
            newContent.setEditorId(userId);
            String editorName = jdbc.queryForObject("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,userId);
            newContent.setEditorName(editorName);
            newContent.setEditTime(LocalDateTime.now());
            docContentMapper.insert(newContent);

            doc.setCurrentVersion(newVersion);
            doc.setLastEditTime(LocalDateTime.now());
            doc.setLastEditorId(userId);
            doc.setLastEditorName(editorName);
            docDocumentMapper.updateById(doc);

            // Atomic increment of edit_count via SQL to avoid read-modify-write race
            docDocumentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DocDocument>()
                            .eq(DocDocument::getDocId, docId)
                            .setSql("edit_count = edit_count + 1"));

            redisTemplate.delete(CACHE_PREFIX + docId);

            // Publish index update event to MQ
            try {
                java.util.Map<String, Object> event = new java.util.HashMap<>();
                event.put("id", docId);
                event.put("title", doc.getTitle());
                event.put("content", content);
                event.put("type", doc.getType() == 2 ? "sheet" : "document");
                event.put("spaceId", doc.getSpaceId());
                event.put("creatorId", doc.getCreatorId());
                event.put("action", "UPDATE");
                rabbitTemplate.convertAndSend("search-index-exchange", "search.document.update", event);
            } catch (Exception e) {
                log.error("Failed to publish content save index event", e);
            }

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public DocumentContentVO getSyncState(String docId) {
        return getContent(docId);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            return "";
        }
    }
}
