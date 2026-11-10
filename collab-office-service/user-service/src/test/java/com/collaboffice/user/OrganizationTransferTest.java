package com.collaboffice.user;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.user.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationTransferTest {
    @Test void ordinaryMemberCannotReceiveOwnershipAndNoDataIsChanged() {
        var jdbc=mock(JdbcTemplate.class);var access=mock(WorkspaceAccess.class);
        when(access.isAdmin(10L)).thenReturn(true);
        when(access.userId()).thenReturn(1L);
        when(access.row(anyString(),eq((Object)10L))).thenReturn(Map.of("owner_id",1L));
        when(jdbc.queryForList(contains("FROM sys_space_member"),eq(10L),eq(2L))).thenReturn(List.of(Map.of("user_id",2L)));
        when(access.platformRole(2L)).thenReturn("MEMBER");
        var error=assertThrows(BusinessException.class,()->new OrganizationService(jdbc,access).transfer(10L,2L));
        assertEquals(400,error.getCode());
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }

    @Test void onlyCurrentOwnerCanTransferEvenIfCallerIsTeamAdmin() {
        var jdbc=mock(JdbcTemplate.class);var access=mock(WorkspaceAccess.class);
        when(access.isAdmin(10L)).thenReturn(true);when(access.userId()).thenReturn(3L);
        when(access.row(anyString(),eq((Object)10L))).thenReturn(Map.of("owner_id",1L));
        var error=assertThrows(BusinessException.class,()->new OrganizationService(jdbc,access).transfer(10L,2L));
        assertEquals(403,error.getCode());
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
