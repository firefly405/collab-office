package com.collaboffice.collab.service.impl;

import com.collaboffice.collab.entity.DocYjsSeq;
import com.collaboffice.collab.entity.DocYjsUpdate;
import com.collaboffice.collab.mapper.DocYjsSeqMapper;
import com.collaboffice.collab.mapper.DocYjsUpdateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CrdtDocumentServiceImpl 单元测试。
 * 覆盖 Yjs update 持久化、查询、seq 分配、RabbitMQ 消费等核心路径。
 */
public class CrdtDocumentServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DocYjsUpdateMapper docYjsUpdateMapper;

    @Mock
    private DocYjsSeqMapper docYjsSeqMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private CrdtDocumentServiceImpl service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        // 注入 @Value 字段
        ReflectionTestUtils.setField(service, "maxUpdatesPerDoc", 500);
    }

    @Test
    public void testPersistYjsUpdate_Success() {
        String docId = "doc_001";
        String clientId = "client_42";
        Long userId = 100L;
        String updateB64 = "AAAB";

        // 模拟 seq 分配：upsert 后 selectById 返回 seq=1
        DocYjsSeq seqRow = new DocYjsSeq();
        seqRow.setDocId(docId);
        seqRow.setSeq(1L);
        when(docYjsSeqMapper.upsertAndIncrement(docId)).thenReturn(1);
        when(docYjsSeqMapper.selectById(docId)).thenReturn(seqRow);
        when(docYjsUpdateMapper.insert(any(DocYjsUpdate.class))).thenReturn(1);

        Long seq = service.persistYjsUpdate(docId, clientId, userId, updateB64, "yjs_update");

        assertNotNull(seq);
        assertEquals(1L, seq);
        verify(docYjsSeqMapper).upsertAndIncrement(docId);
        verify(docYjsUpdateMapper).insert(argThat(r ->
                docId.equals(r.getDocId())
                        && clientId.equals(r.getClientId())
                        && userId.equals(r.getUserId())
                        && updateB64.equals(r.getUpdateB64())
                        && seq.equals(r.getSeq())
                        && "yjs_update".equals(r.getKind())
        ));
        // 验证 Redis 缓存写入
        verify(valueOperations).set(eq("doc:version:" + docId), eq(1L), anyLong(), any());
    }

    @Test
    public void testPersistYjsUpdate_NullInputs() {
        assertNull(service.persistYjsUpdate(null, "c", 1L, "u", "yjs_update"));
        assertNull(service.persistYjsUpdate("doc", "c", 1L, null, "yjs_update"));
        assertNull(service.persistYjsUpdate("doc", "c", 1L, "", "yjs_update"));
    }

    @Test
    public void testPersistYjsUpdate_SeqAllocationFails() {
        // selectById 返回 null，应返回 null 且不 insert
        when(docYjsSeqMapper.upsertAndIncrement(anyString())).thenReturn(1);
        when(docYjsSeqMapper.selectById(anyString())).thenReturn(null);

        Long seq = service.persistYjsUpdate("doc_x", "c", 1L, "u", "yjs_update");
        assertNull(seq);
        verify(docYjsUpdateMapper, never()).insert(any());
    }

    @Test
    public void testGetYjsUpdates_FullSync() {
        String docId = "doc_001";
        DocYjsUpdate u1 = buildUpdate(docId, "c1", 1L, "AAA", 1L);
        DocYjsUpdate u2 = buildUpdate(docId, "c2", 2L, "BBB", 2L);
        when(docYjsUpdateMapper.selectAll(docId, 500)).thenReturn(Arrays.asList(u1, u2));

        List<Map<String, Object>> result = service.getYjsUpdates(docId, 0L, 500);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).get("seq"));
        assertEquals("AAA", result.get(0).get("update"));
        assertEquals(2L, result.get(1).get("seq"));
        verify(docYjsUpdateMapper).selectAll(docId, 500);
        verify(docYjsUpdateMapper, never()).selectAfterSeq(anyString(), anyLong(), anyInt());
    }

    @Test
    public void testGetYjsUpdates_IncrementalSync() {
        String docId = "doc_001";
        DocYjsUpdate u3 = buildUpdate(docId, "c3", 3L, "CCC", 3L);
        when(docYjsUpdateMapper.selectAfterSeq(docId, 2L, 500)).thenReturn(Collections.singletonList(u3));

        List<Map<String, Object>> result = service.getYjsUpdates(docId, 2L, 500);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).get("seq"));
        verify(docYjsUpdateMapper).selectAfterSeq(docId, 2L, 500);
        verify(docYjsUpdateMapper, never()).selectAll(anyString(), anyInt());
    }

    @Test
    public void testGetYjsUpdates_EmptyResult() {
        when(docYjsUpdateMapper.selectAll(anyString(), anyInt())).thenReturn(Collections.emptyList());
        List<Map<String, Object>> result = service.getYjsUpdates("empty_doc", 0L, 500);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetYjsMaxSeq_CacheHit() {
        // Redis 缓存命中，不应查询 DB
        when(valueOperations.get("doc:version:doc_001")).thenReturn(42L);

        long maxSeq = service.getYjsMaxSeq("doc_001");

        assertEquals(42L, maxSeq);
        verify(docYjsUpdateMapper, never()).selectMaxSeq(anyString());
    }

    @Test
    public void testGetYjsMaxSeq_CacheMiss() {
        // Redis 缓存未命中，查询 DB
        when(valueOperations.get("doc:version:doc_001")).thenReturn(null);
        when(docYjsUpdateMapper.selectMaxSeq("doc_001")).thenReturn(99L);

        long maxSeq = service.getYjsMaxSeq("doc_001");

        assertEquals(99L, maxSeq);
        verify(docYjsUpdateMapper).selectMaxSeq("doc_001");
        // 验证写回缓存
        verify(valueOperations).set(eq("doc:version:doc_001"), eq(99L), anyLong(), any());
    }

    @Test
    public void testOnYjsUpdateMessage_ValidPayload() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("docId", "doc_msg");
        msg.put("clientId", "client_x");
        msg.put("userId", 5L);
        msg.put("update", "AAAB");
        msg.put("kind", "yjs_update");

        DocYjsSeq seqRow = new DocYjsSeq();
        seqRow.setDocId("doc_msg");
        seqRow.setSeq(7L);
        when(docYjsSeqMapper.upsertAndIncrement("doc_msg")).thenReturn(1);
        when(docYjsSeqMapper.selectById("doc_msg")).thenReturn(seqRow);
        when(docYjsUpdateMapper.insert(any())).thenReturn(1);

        service.onYjsUpdateMessage(msg);

        verify(docYjsSeqMapper).upsertAndIncrement("doc_msg");
        verify(docYjsUpdateMapper).insert(argThat(r -> "doc_msg".equals(r.getDocId()) && 7L == r.getSeq()));
    }

    @Test
    public void testOnYjsUpdateMessage_InvalidPayload() {
        // 缺少 docId 和 update，应直接返回不持久化
        Map<String, Object> msg = new HashMap<>();
        msg.put("clientId", "c");

        service.onYjsUpdateMessage(msg);

        verify(docYjsSeqMapper, never()).upsertAndIncrement(anyString());
        verify(docYjsUpdateMapper, never()).insert(any());
    }

    @Test
    public void testOnYjsUpdateMessage_NullMessage() {
        service.onYjsUpdateMessage(null);
        verifyNoInteractions(docYjsSeqMapper, docYjsUpdateMapper);
    }

    @Test
    public void testJoinDocument_AddsToRedisSet() {
        service.joinDocument("doc_1", "user_1");
        verify(setOperations).add("doc:users:doc_1", "user_1");
        verify(redisTemplate).expire(eq("doc:users:doc_1"), anyLong(), any());
    }

    @Test
    public void testLeaveDocument_RemovesFromRedisSet() {
        service.leaveDocument("doc_1", "user_1");
        verify(setOperations).remove("doc:users:doc_1", "user_1");
    }

    @Test
    public void testGetOnlineUsers() {
        java.util.Set<Object> mockMembers = new java.util.HashSet<>(Arrays.asList("u1", "u2"));
        when(setOperations.members("doc:users:doc_1")).thenReturn(mockMembers);

        java.util.Set<Object> result = service.getOnlineUsers("doc_1");

        assertEquals(2, result.size());
        assertTrue(result.contains("u1"));
        assertTrue(result.contains("u2"));
    }

    @Test
    public void testGetOnlineUsers_Empty() {
        when(setOperations.members(anyString())).thenReturn(null);
        java.util.Set<Object> result = service.getOnlineUsers("no_users_doc");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private DocYjsUpdate buildUpdate(String docId, String clientId, Long userId,
                                      String updateB64, Long seq) {
        DocYjsUpdate u = new DocYjsUpdate();
        u.setDocId(docId);
        u.setClientId(clientId);
        u.setUserId(userId);
        u.setUpdateB64(updateB64);
        u.setSeq(seq);
        u.setKind("yjs_update");
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }
}
