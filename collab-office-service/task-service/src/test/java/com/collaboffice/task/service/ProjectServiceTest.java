package com.collaboffice.task.service;

import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock WorkspaceAccess access;
    @Mock JdbcTemplate jdbc;
    @Mock TaskService tasks;
    @InjectMocks ProjectService service;
    Map<String,Object> project(long creator) {return Map.of("project_id","100","space_id",15L,"creator_id",creator,"name","Demo","status",1);}
    @Test void nonManagerCannotArchiveOrChangeMembers() {
        when(access.row(anyString(),eq("100"))).thenReturn(project(2));
        doAnswer(invocation->{if(Boolean.TRUE.equals(invocation.getArgument(1)))throw new BusinessException(403,"仅项目管理员可操作");return null;}).when(access).requireProject(eq("100"),anyBoolean());
        var error=assertThrows(BusinessException.class,()->service.update("100",Map.of("status",2,"memberIds",List.of(1))));
        assertEquals(403,error.getCode());verifyNoInteractions(jdbc);
    }
    @Test void memberFromAnotherTeamIsRejectedBeforeAnyMutation() {
        when(access.row(anyString(),eq("100"))).thenReturn(project(1));
        when(jdbc.queryForObject(contains("sys_space_member"),eq(Integer.class),eq(15L),eq(9L))).thenReturn(0);
        assertThrows(BusinessException.class,()->service.update("100",Map.of("memberIds",List.of(9))));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void projectManagerCannotTransferResponsibility() {
        when(access.row(anyString(),eq("100"))).thenReturn(project(1));
        doThrow(new BusinessException(403,"Only system administrator")).when(access).requireSystemAdmin();
        assertThrows(BusinessException.class,()->service.update("100",Map.of("managerId","2")));
        verifyNoInteractions(jdbc);
    }
    @Test void ordinaryUserCannotBeAssignedAsProjectManager() {
        when(access.row(anyString(),eq("100"))).thenReturn(project(1));
        when(access.platformRole(2L)).thenReturn("MEMBER");
        assertThrows(BusinessException.class,()->service.update("100",Map.of("managerId","2")));
        verifyNoInteractions(jdbc);
    }
}
