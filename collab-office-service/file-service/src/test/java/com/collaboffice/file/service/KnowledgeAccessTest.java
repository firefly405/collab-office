package com.collaboffice.file.service;

import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.file.entity.KnowledgeItem;
import com.collaboffice.file.mapper.KnowledgeItemMapper;
import com.collaboffice.file.service.impl.KnowledgeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeAccessTest {
    @Mock KnowledgeItemMapper items;
    @Mock WorkspaceAccess access;
    @Mock JdbcTemplate jdbc;
    @InjectMocks KnowledgeServiceImpl service;
    @Test void otherContributorsCannotOverwriteKnowledge() {
        var old=new KnowledgeItem();old.setSpaceId(13L);old.setContributorId(2L);
        when(items.selectOne(any())).thenReturn(old);when(access.userId()).thenReturn(1L);
        assertThrows(BusinessException.class,()->service.updateKnowledge("item",new KnowledgeItem()));
        verify(items,never()).updateById(any());
    }
    @Test void crossTeamDetailIsRejectedBeforeViewIncrement() {
        var old=new KnowledgeItem();old.setSpaceId(14L);when(items.selectOne(any())).thenReturn(old);
        doThrow(new BusinessException(403,"forbidden")).when(access).requireSpace(14L,false);
        assertThrows(BusinessException.class,()->service.getKnowledgeDetail("item"));verify(items,never()).updateById(any());
    }
}
