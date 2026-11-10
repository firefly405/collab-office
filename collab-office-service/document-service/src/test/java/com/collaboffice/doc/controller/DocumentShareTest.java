package com.collaboffice.doc.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.collaboffice.doc.dto.DocumentShareRequest;
import com.collaboffice.doc.entity.DocShare;
import com.collaboffice.doc.mapper.DocDocumentMapper;
import com.collaboffice.doc.mapper.DocShareMapper;
import com.collaboffice.doc.service.DocumentContentService;
import com.collaboffice.doc.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentShareTest {
    @Mock DocShareMapper shares;
    @Mock DocDocumentMapper documents;
    @Mock DocumentService service;
    @Mock DocumentContentService content;
    @Mock org.springframework.jdbc.core.JdbcTemplate jdbc;
    @InjectMocks DocumentController controller;

    @Test void newShareStoresHashAndForcesReadOnly() {
        var request = new DocumentShareRequest();
        request.setPermission(2);
        request.setPassword("local-check");
        controller.createShare(1L, "123", request);
        var capture = ArgumentCaptor.forClass(DocShare.class);
        verify(shares).insert(capture.capture());
        assertEquals(1, capture.getValue().getPermission());
        assertNotEquals("local-check", capture.getValue().getSharePassword());
        assertTrue(BCrypt.checkpw("local-check", capture.getValue().getSharePassword()));
    }

    @Test void passwordCheckedBeforeReturningContent() {
        var share = activeShare();
        share.setSharePassword(BCrypt.hashpw("local-check"));
        when(shares.selectByShareToken("token")).thenReturn(share);
        when(documents.selectCount(any())).thenReturn(1L);
        when(jdbc.queryForObject(anyString(),eq(Integer.class),eq("123"))).thenReturn(1);
        assertEquals(401, controller.getShareContent("token", "wrong").getCode());
        verifyNoInteractions(content);
        assertEquals(200, controller.getShareContent("token", "local-check").getCode());
        verify(content).getContent("123");
    }

    @Test void expiredOrRecycledDocumentCannotBeShared() {
        var share = activeShare();
        when(shares.selectByShareToken("token")).thenReturn(share);
        when(documents.selectCount(any())).thenReturn(0L);
        assertEquals(404, controller.getShareContent("token", null).getCode());
        share.setExpireTime(LocalDateTime.now().minusMinutes(1));
        assertEquals(404, controller.getShare("token", null).getCode());
        verifyNoInteractions(content, service);
    }

    private DocShare activeShare() {
        var share = new DocShare();
        share.setDocId("123");
        share.setStatus(1);
        return share;
    }
    @Test void dissolvedSpaceCannotUseOldShare(){when(shares.selectByShareToken("token")).thenReturn(activeShare());when(documents.selectCount(any())).thenReturn(1L);when(jdbc.queryForObject(anyString(),eq(Integer.class),eq("123"))).thenReturn(0);assertEquals(404,controller.getShareContent("token",null).getCode());verifyNoInteractions(content);}
}
