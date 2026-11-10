package com.collaboffice.msg;

import com.collaboffice.msg.entity.ChatMessage;
import com.collaboffice.msg.entity.ChatSessionMember;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collaboffice.msg.mapper.ChatMessageMapper;
import com.collaboffice.msg.mapper.ChatSessionMemberMapper;
import com.collaboffice.msg.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OfflineMessageTest {

    @Mock
    private ChatSessionMemberMapper sessionMemberMapper;

    @Mock
    private ChatMessageMapper messageMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetOfflineMessages_NoSessions() {
        when(sessionMemberMapper.selectByUserId(1L)).thenReturn(Collections.emptyList());
        List<Map<String, Object>> result = chatService.getOfflineMessages(1L, 0L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetOfflineMessages_HasOfflineMessages() {
        ChatSessionMember member = new ChatSessionMember();
        member.setSessionId("session_123");
        member.setUserId(1L);

        ChatMessage msg = new ChatMessage();
        msg.setMsgId("msg_1");
        msg.setSessionId("session_123");
        msg.setSenderId(2L);
        msg.setContent("Hello offline");
        msg.setMsgType(1);
        msg.setSendTime(LocalDateTime.now());
        msg.setSeqId(10L);

        when(sessionMemberMapper.selectByUserId(1L)).thenReturn(Collections.singletonList(member));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(msg));

        List<Map<String, Object>> result = chatService.getOfflineMessages(1L, 5L);

        assertEquals(1, result.size());
        assertEquals("msg_1", result.get(0).get("msgId"));
        assertEquals("Hello offline", result.get(0).get("content"));
        assertEquals(10L, result.get(0).get("seqId"));
    }
}
