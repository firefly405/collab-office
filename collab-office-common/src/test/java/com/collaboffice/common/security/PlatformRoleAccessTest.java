package com.collaboffice.common.security;

import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformRoleAccessTest {
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final WorkspaceAccess access=new WorkspaceAccess(jdbc);
    @BeforeEach void setup(){
        var request=new MockHttpServletRequest();request.addHeader("X-User-Id","2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(jdbc.queryForList(contains("m.role_type"),eq(String.class),eq(15L),eq(2L))).thenReturn(List.of("2"));
        when(jdbc.queryForList(contains("r.permissions_json"),eq(15L),eq(2L))).thenReturn(List.of(Map.of("role_type","2")));
        when(jdbc.queryForObject(contains("task_project_member"),eq(Integer.class),eq("100"),eq(2L))).thenReturn(0);
        project(2L);
    }
    @AfterEach void clear(){RequestContextHolder.resetRequestAttributes();}
    void role(String role){when(jdbc.queryForList(contains("r.role_code"),eq(String.class),eq(2L))).thenReturn(List.of(role));}
    void project(Long creator){when(jdbc.queryForList(contains("SELECT space_id,creator_id FROM task_project"),eq((Object)"100"))).thenReturn(List.of(Map.of("space_id",15L,"creator_id",creator)));}
    @Test void defaultUserCannotCreateProjectsOrManageEvenOwnedProject(){
        assertEquals("MEMBER",access.platformRole(2L));
        assertThrows(BusinessException.class,access::requireProjectCreator);
        assertThrows(BusinessException.class,()->access.requireProject("100",true));
        assertThrows(BusinessException.class,access::requireSystemAdmin);
    }
    @Test void projectManagerManagesOnlyOwnedProject(){
        role("project_admin");assertDoesNotThrow(()->access.requireProject("100",true));
        project(3L);assertThrows(BusinessException.class,()->access.requireProject("100",true));
        assertThrows(BusinessException.class,access::requireSystemAdmin);
    }
    @Test void projectMemberCanBrowseButCannotManage(){
        project(3L);when(jdbc.queryForObject(contains("task_project_member"),eq(Integer.class),eq("100"),eq(2L))).thenReturn(1);
        assertDoesNotThrow(()->access.requireProject("100",false));
        assertThrows(BusinessException.class,()->access.requireProject("100",true));
    }
    @Test void teamMembershipAloneDoesNotExposeProject(){
        project(3L);assertFalse(access.canReadProject("100"));
    }
    @Test void systemAdminStillNeedsActiveTeamMembership(){
        role("admin");assertDoesNotThrow(()->access.requireProject("100",true));
        when(jdbc.queryForList(contains("m.role_type"),eq(String.class),eq(15L),eq(2L))).thenReturn(List.of());
        assertThrows(BusinessException.class,()->access.requireProject("100",false));
    }
    @Test void assignedUserCanUpdateButAnotherMemberCannot(){
        when(jdbc.queryForList(contains("SELECT t.creator_id,t.assignee_id"),eq((Object)"200"))).thenReturn(List.of(Map.of("space_id",15L,"project_id","100","creator_id",3L,"assignee_id",2L)));
        assertDoesNotThrow(()->access.requireTask("200",true));
        when(jdbc.queryForList(contains("SELECT t.creator_id,t.assignee_id"),eq((Object)"200"))).thenReturn(List.of(Map.of("space_id",15L,"project_id","100","creator_id",3L,"assignee_id",3L)));
        assertDoesNotThrow(()->access.requireTask("200",false));
        assertThrows(BusinessException.class,()->access.requireTask("200",true));
    }
    @Test void removedProjectMemberLosesReadAccess(){project(3L);assertThrows(BusinessException.class,()->access.requireProject("100",false));}
}
