package com.collaboffice.collab.service.impl;

import com.collaboffice.collab.config.RabbitMQConfig;
import com.collaboffice.collab.entity.DocYjsUpdate;
import com.collaboffice.collab.mapper.DocYjsSeqMapper;
import com.collaboffice.collab.mapper.DocYjsUpdateMapper;
import com.collaboffice.collab.service.CollaborationService;
import com.collaboffice.collab.entity.DocYjsSeq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协同文档服务实现。
 *
 * 改造说明（P0-4）：
 *  - Yjs CRDT 算法本身仍由前端 Y.Doc 完成（Y.applyUpdate / Y.encodeStateAsUpdate）
 *  - 本服务负责真正的 update 持久化：每条 update 落 MySQL {@code doc_yjs_update} 表，
 *    并分配单调递增的 seq（由 {@code doc_yjs_seq} 表的 upsert 原子操作产生）
 *  - 新客户端 join 文档时可通过 {@link #getYjsUpdates} 拉取历史 updates 做初始化同步
 *  - WS 服务端在 {@code handleDocOperation} 时异步转发 update 到本服务的 RabbitMQ 队列，
 *    本服务通过 {@link #onYjsUpdateMessage} 消费并持久化
 *  - Redis 仅保留在线用户列表与轻量缓存，不再作为操作日志存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrdtDocumentServiceImpl implements CollaborationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final DocYjsUpdateMapper docYjsUpdateMapper;
    private final DocYjsSeqMapper docYjsSeqMapper;

    private static final String USERS_PREFIX = "doc:users:";
    private static final String VERSION_PREFIX = "doc:version:";

    @Value("${app.collab.max-updates-per-doc:500}")
    private int maxUpdatesPerDoc;

    // ============== 兼容旧接口（HTTP /operation 仍可调用） ==============

    @Override
    public void applyOperation(String docId, String userId, String operationJson) {
        // 兼容旧调用方：解析 JSON 后委托给 persistYjsUpdate
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> op = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(operationJson, Map.class);
            String clientId = op.get("clientId") == null ? "unknown" : String.valueOf(op.get("clientId"));
            String updateB64 = op.get("update") == null ? null : String.valueOf(op.get("update"));
            String kind = op.get("kind") == null ? "yjs_update" : String.valueOf(op.get("kind"));
            Long uid = parseLong(userId);
            if (updateB64 != null) {
                persistYjsUpdate(docId, clientId, uid, updateB64, kind);
            }
        } catch (Exception e) {
            log.warn("Failed to parse legacy operation JSON for docId={}: {}", docId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDocumentState(String docId) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("docId", docId);
        state.put("maxSeq", getYjsMaxSeq(docId));
        state.put("onlineUsers", getOnlineUsers(docId));
        return state;
    }

    @Override
    public void joinDocument(String docId, String userId) {
        String key = USERS_PREFIX + docId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        log.debug("User {} joined document {}", userId, docId);
    }

    @Override
    public void leaveDocument(String docId, String userId) {
        String key = USERS_PREFIX + docId;
        redisTemplate.opsForSet().remove(key, userId);
        log.debug("User {} left document {}", userId, docId);
    }

    @Override
    public Set<Object> getOnlineUsers(String docId) {
        Set<Object> members = redisTemplate.opsForSet().members(USERS_PREFIX + docId);
        return members != null ? members : Collections.emptySet();
    }

    @Override
    public void persistDocumentState(String docId) {
        // 旧接口：现在持久化是实时的（每条 update 即时落库），此处仅做日志与可选的快照压缩
        long maxSeq = getYjsMaxSeq(docId);
        log.info("Persist check for docId={}, currentMaxSeq={}", docId, maxSeq);
        try {
            compactIfNeeded(docId);
        } catch (Exception e) {
            log.warn("Compact failed for docId={}: {}", docId, e.getMessage());
        }
    }

    // ============== 新接口：真正的 Yjs update 持久化 ==============

    @Override
    @Transactional
    public Long persistYjsUpdate(String docId, String clientId, Long userId, String updateB64, String kind) {
        if (docId == null || updateB64 == null || updateB64.isEmpty()) {
            return null;
        }
        try {
            // 1. 原子分配下一个 seq
            docYjsSeqMapper.upsertAndIncrement(docId);
            DocYjsSeq seqRow = docYjsSeqMapper.selectById(docId);
            if (seqRow == null || seqRow.getSeq() == null) {
                log.error("Failed to allocate seq for docId={}: seqRow is null", docId);
                return null;
            }
            long seq = seqRow.getSeq();

            // 2. 写入 update 记录
            DocYjsUpdate record = new DocYjsUpdate();
            record.setDocId(docId);
            record.setClientId(clientId != null ? clientId : "unknown");
            record.setUserId(userId);
            record.setUpdateB64(updateB64);
            record.setSeq(seq);
            record.setKind(kind != null ? kind : "yjs_update");
            record.setCreatedAt(LocalDateTime.now());
            docYjsUpdateMapper.insert(record);

            // 3. 更新 Redis 中的 version 缓存（轻量）
            redisTemplate.opsForValue().set(VERSION_PREFIX + docId, seq, 2, TimeUnit.HOURS);

            log.debug("Persisted Yjs update: docId={} seq={} clientId={} kind={}", docId, seq, clientId, record.getKind());
            return seq;
        } catch (Exception e) {
            log.error("Failed to persist Yjs update for docId={}: {}", docId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getYjsUpdates(String docId, long afterSeq, int limit) {
        if (limit <= 0) limit = 500;
        List<DocYjsUpdate> records;
        if (afterSeq <= 0) {
            records = docYjsUpdateMapper.selectAll(docId, limit);
        } else {
            records = docYjsUpdateMapper.selectAfterSeq(docId, afterSeq, limit);
        }
        if (records == null || records.isEmpty()) return Collections.emptyList();
        return records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("seq", r.getSeq());
            m.put("clientId", r.getClientId());
            m.put("userId", r.getUserId());
            m.put("update", r.getUpdateB64());
            m.put("kind", r.getKind());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public long getYjsMaxSeq(String docId) {
        Object cached = redisTemplate.opsForValue().get(VERSION_PREFIX + docId);
        if (cached instanceof Number) return ((Number) cached).longValue();
        Long max = docYjsUpdateMapper.selectMaxSeq(docId);
        long v = max != null ? max : 0L;
        redisTemplate.opsForValue().set(VERSION_PREFIX + docId, v, 2, TimeUnit.HOURS);
        return v;
    }

    // ============== RabbitMQ 消费：来自 WS 的 Yjs update ==============

    /**
     * 消费 WS 服务端转发的 Yjs update 消息，落库持久化。
     * 消息体字段：docId, clientId, userId, update, kind
     */
    @RabbitListener(queues = RabbitMQConfig.YJS_PERSIST_QUEUE)
    public void onYjsUpdateMessage(Map<String, Object> msg) {
        if (msg == null) return;
        String docId = asString(msg.get("docId"));
        String clientId = asString(msg.get("clientId"));
        Long userId = parseLong(msg.get("userId"));
        String updateB64 = asString(msg.get("update"));
        String kind = asString(msg.get("kind"));
        if (docId == null || updateB64 == null) {
            log.warn("Invalid Yjs update message, missing docId or update: {}", msg);
            return;
        }
        persistYjsUpdate(docId, clientId, userId, updateB64, kind);
    }

    // ============== 内部工具 ==============

    /**
     * 当单文档 update 数超过阈值时，删除最旧的一部分（保留最新 N 条）。
     * 注意：这会导致老客户端若长时间离线后重新 join，需要靠最新的 yjs_state 全量同步
     * 而非增量 update。生产环境应改为合并为快照，这里先做简单裁剪。
     */
    private void compactIfNeeded(String docId) {
        Long maxSeq = docYjsUpdateMapper.selectMaxSeq(docId);
        if (maxSeq == null || maxSeq < maxUpdatesPerDoc) return;

        long cutoff = maxSeq - maxUpdatesPerDoc / 2;
        try {
            long deleted = docYjsUpdateMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocYjsUpdate>()
                            .eq(DocYjsUpdate::getDocId, docId)
                            .lt(DocYjsUpdate::getSeq, cutoff));
            if (deleted > 0) {
                log.info("Compacted {} old Yjs updates for docId={} (kept seq>={})", deleted, docId, cutoff);
            }
        } catch (Exception e) {
            log.warn("Compact failed for docId={}: {}", docId, e.getMessage());
        }
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long parseLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
