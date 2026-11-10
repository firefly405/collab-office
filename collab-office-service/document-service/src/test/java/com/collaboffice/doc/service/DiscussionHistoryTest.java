package com.collaboffice.doc.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.CommentCreateRequest;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DiscussionHistoryTest {
    final DocCommentMapper mapper=mock(DocCommentMapper.class);
    final WorkspaceAccess access=mock(WorkspaceAccess.class);
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final CommentServiceImpl comments=new CommentServiceImpl(mapper,access,jdbc,mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class));

    DocComment comment(long id,Long parent,int resolved){var c=new DocComment();c.setId(id);c.setDocId("10");c.setParentId(parent);c.setIsResolved(resolved);return c;}

    @Test void threadFilterUsesRootAndKeepsNestedReplies(){
        when(mapper.selectByDocId("10")).thenReturn(List.of(comment(1,null,1),comment(2,1L,0),comment(3,2L,0),comment(4,null,0)));
        assertEquals(1,comments.listByDocId("10","resolved").size());
        assertEquals(3L,comments.listByDocId("10","resolved").get(0).getReplies().get(0).getReplies().get(0).getCommentId());
        assertEquals(4L,comments.listByDocId("10","unresolved").get(0).getCommentId());
    }

    @Test void crossDocumentReplyAndMalformedAnchorRejected(){
        when(mapper.selectById(1L)).thenReturn(comment(1,null,0));
        var r=new CommentCreateRequest();r.setDocId("11");r.setContent("reply");r.setParentId(1L);
        assertThrows(BusinessException.class,()->comments.create(r,2L,"user"));
        r.setParentId(null);r.setSelectionRange("{\"index\":-1,\"length\":1,\"text\":\"x\"}");
        assertThrows(BusinessException.class,()->comments.create(r,2L,"user"));
        verify(mapper,never()).insert(any(DocComment.class));
    }

    @Test void foreignMentionRejectedBeforeCommentWrite(){
        var r=new CommentCreateRequest();r.setDocId("10");r.setContent("hello");r.setMentionUsers(List.of(9L));
        doThrow(new BusinessException(403,"forbidden")).when(access).requireDocument("10",9L,false);
        assertThrows(BusinessException.class,()->comments.create(r,2L,"user"));verify(mapper,never()).insert(any(DocComment.class));
    }

    @Test void retentionAndMetadataValidatedWithoutDeletingContent(){
        var history=new DocumentHistoryService(jdbc,access);
        assertThrows(BusinessException.class,()->history.policy("10",9));
        assertThrows(BusinessException.class,()->history.name("10",1L," ",""));
        history.policy("10",20);verify(jdbc).update("UPDATE doc_document SET version_retention=? WHERE doc_id=?",20,"10");
        verifyNoMoreInteractions(jdbc);
    }

    @Test void diffComparesReadableTextAndFailsMissingVersion(){
        var contents=mock(DocContentMapper.class);
        var service=new DocumentServiceImpl(null,jdbc,access,null,contents,null,null,null,null,null,null,null,null);
        var old=new DocContent();old.setContent("<h1>计划</h1><p>旧正文</p>");
        var updated=new DocContent();updated.setContent("<h1>计划</h1><p>新正文 &amp; 内容</p>");
        when(contents.selectByDocIdAndVersion("10",1L)).thenReturn(old);when(contents.selectByDocIdAndVersion("10",2L)).thenReturn(updated);
        var diff=service.compareVersions("10",1L,2L).getDiffSegments();
        assertTrue(diff.stream().anyMatch(s->s.getType().equals("insert")&&s.getText().equals("新正文 & 内容")));
        assertTrue(diff.stream().anyMatch(s->s.getType().equals("delete")&&s.getText().equals("旧正文")));
        assertThrows(BusinessException.class,()->service.compareVersions("10",1L,3L));
    }
}
