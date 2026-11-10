package com.collaboffice.user.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.user.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagementSafetyTest {
    @Test void activeTeamOwnerCannotBeDemoted() {
        var jdbc=mock(JdbcTemplate.class);var access=mock(WorkspaceAccess.class);
        when(access.userId()).thenReturn(1L);
        when(jdbc.queryForList(contains("SELECT ur.user_id"),eq(Long.class))).thenReturn(List.of(1L,2L));
        when(jdbc.queryForList(contains("FROM sys_user WHERE"),eq((Object)2L))).thenReturn(List.of(Map.of("id",2L)));
        when(jdbc.queryForObject(contains("FROM sys_space"),eq(Integer.class),eq(2L))).thenReturn(1);
        var controller=new PlatformRoleController(access,jdbc,mock(org.springframework.data.redis.core.RedisTemplate.class));
        var error=assertThrows(BusinessException.class,()->controller.change(2L,Map.of("platformRole","MEMBER")));
        assertEquals(409,error.getCode());
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }

    @Test void dashboardCountsManagementAuditInsteadOfEmptySystemLog() {
        var jdbc=mock(JdbcTemplate.class);var logs=mock(SysLogMapper.class);
        when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM workspace_audit"),eq(Long.class))).thenReturn(12L);
        when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM workspace_audit WHERE created_at>=?"),eq(Long.class),any())).thenReturn(3L);
        when(jdbc.queryForObject(contains("created_at<?"),eq(Long.class),any(),any())).thenReturn(2L);
        var result=new AdminController(logs,mock(SysUserMapper.class),mock(SysSpaceMapper.class),null,jdbc).dashboardStats().getData();
        assertEquals(3L,result.get("todayOperations"));assertEquals(12L,result.get("totalLogs"));
        assertEquals(7,((List<?>)result.get("trend")).size());
        assertEquals(2L,((Map<?,?>)((List<?>)result.get("trend")).get(0)).get("count"));
        verifyNoInteractions(logs);
    }

    @Test void ordinaryProjectRoleCanStillChangeWithoutAdministratorOwnershipGuard() {
        var jdbc=mock(JdbcTemplate.class);var access=mock(WorkspaceAccess.class);
        when(access.userId()).thenReturn(1L);
        when(jdbc.queryForList(contains("SELECT ur.user_id"),eq(Long.class))).thenReturn(List.of(1L));
        when(jdbc.queryForList(contains("FROM sys_user WHERE"),eq((Object)2L))).thenReturn(List.of(Map.of("id",2L)));
        var controller=new PlatformRoleController(access,jdbc,mock(org.springframework.data.redis.core.RedisTemplate.class));
        assertDoesNotThrow(()->controller.change(2L,Map.of("platformRole","MEMBER")));
        verify(jdbc).update(startsWith("INSERT INTO sys_user_role"),eq(2L),eq("member"));
    }
}
