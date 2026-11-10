package com.collaboffice.msg;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.msg.entity.ChatMessage;
import com.collaboffice.msg.mapper.ChatMessageMapper;
import com.collaboffice.msg.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MessageRecallTest {
    @Mock ChatMessageMapper messages;
    @Mock WorkspaceAccess access;
    @Mock JdbcTemplate jdbc;
    @InjectMocks ChatServiceImpl chat;
    ChatMessage message() {var m=new ChatMessage();m.setSenderId(1L);m.setSessionId("100");m.setSendTime(LocalDateTime.now());return m;}
    @Test void ownRecentMessageCanBeRecalled() {
        var m=message();when(messages.selectOne(any())).thenReturn(m);chat.recallMessage(1L,"1");
        assertEquals(1,m.getIsRecalled());verify(messages).updateById(m);verify(access).requireSession("100");
    }
    @Test void oldMessagesCannotBeRecalled() {
        var m=message();m.setSendTime(LocalDateTime.now().minusMinutes(3));when(messages.selectOne(any())).thenReturn(m);
        assertThrows(BusinessException.class,()->chat.recallMessage(1L,"1"));verify(messages,never()).updateById(any());
    }
    @Test void otherAuthorsCannotBeRecalled() {
        when(messages.selectOne(any())).thenReturn(message());assertThrows(BusinessException.class,()->chat.recallMessage(2L,"1"));
        verify(messages,never()).updateById(any());
    }
    @Test void quotingMissingMessageDoesNotSend() {
        when(jdbc.queryForObject(anyString(),eq(Integer.class),eq("missing"),eq("100"))).thenReturn(0);
        assertThrows(BusinessException.class,()->chat.sendReply("100",1L,"reply",1,"missing"));verifyNoInteractions(messages);
    }
    @Test void recalledDocumentDiscussionDoesNotExposeOriginalContent() {
        var m=message();m.setIsRecalled(1);m.setContent("original private content");
        when(messages.selectList(any())).thenReturn(java.util.List.of(m));
        var result=chat.getComments("doc","100");
        assertEquals("此消息已撤回",result.get(0).get("content"));
        assertEquals(1,result.get(0).get("isRecalled"));
    }
}
