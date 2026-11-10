package com.collaboffice.common.security;

import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceAccessTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final WorkspaceAccess access = new WorkspaceAccess(jdbc);
    @BeforeEach void request() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(jdbc.queryForList(contains("r.permissions_json"),eq(1L),eq(2L))).thenReturn(List.of(Map.of("role_type","2")));
    }
    @AfterEach void clear() { RequestContextHolder.resetRequestAttributes(); }

    @Test void rejectsOtherUsersUnscopedFile() {
        when(jdbc.queryForList(anyString(), eq((Object)"10"))).thenReturn(List.of(Map.of("uploader_id", 1L)));
        assertThrows(BusinessException.class, () -> access.requireFile("10", false));
    }
    @Test void readonlyDocumentMemberCannotSave() {
        when(jdbc.queryForList(anyString(), eq((Object)"10"))).thenReturn(List.of(Map.of("creator_id", 1L, "space_id", 1L)));
        when(jdbc.queryForList(anyString(), eq(Integer.class), eq("10"), eq(2L))).thenReturn(List.of(1));
        assertDoesNotThrow(() -> access.requireDocument("10", false));
        assertThrows(BusinessException.class, () -> access.requireDocument("10", true));
    }
    @Test void outsiderCannotEnterSpace() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq(1L), eq(2L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> access.requireSpace(1L, false));
    }

    @Test void commentOnlyMemberCannotSaveBody() {
        when(jdbc.queryForList(anyString(), eq((Object)"10"))).thenReturn(List.of(Map.of("creator_id",1L,"space_id",1L)));
        when(jdbc.queryForList(anyString(),eq(Integer.class),eq("10"),eq(2L))).thenReturn(List.of(4));
        assertDoesNotThrow(()->access.requireComment("10"));
        assertThrows(BusinessException.class,()->access.requireDocument("10",true));
    }
    @Test void visitorCanReadButCannotWrite() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq(1L), eq(2L))).thenReturn(List.of("3"));
        assertDoesNotThrow(() -> access.requireSpace(1L, false));
        assertThrows(BusinessException.class, () -> access.requireSpace(1L, true));
    }
    @Test void customRoleCeilingAppliesToDocumentOwner(){
        when(jdbc.queryForList(anyString(),eq((Object)"10"))).thenReturn(List.of(Map.of("creator_id",2L,"space_id",1L)));
        when(jdbc.queryForList(contains("r.permissions_json"),eq(1L),eq(2L))).thenReturn(List.of(Map.of("role_type","2","permissions_json","{\"view\":true,\"edit\":false,\"download\":false}")));
        assertDoesNotThrow(()->access.requireDocument("10",false));assertThrows(BusinessException.class,()->access.requireDocument("10",true));assertThrows(BusinessException.class,()->access.requireDocumentOperation("10","download"));
    }
    @Test void expiredOrDissolvedMembershipIsRejected(){when(jdbc.queryForList(contains("r.permissions_json"),eq(1L),eq(2L))).thenReturn(List.of());assertThrows(BusinessException.class,()->access.requireOperation(1L,2L,"view"));}
    @Test void nearestFolderRuleRestrictsAndInherits(){
        when(jdbc.queryForList(contains("parent_id,space_id"),eq((Object)"20"))).thenReturn(List.of(Map.of("parent_id","10","space_id",1L,"creator_id",1L)));
        when(jdbc.queryForList(contains("parent_id,space_id"),eq((Object)"10"))).thenReturn(List.of(Map.of("space_id",1L,"creator_id",1L)));
        when(jdbc.queryForList(contains("FROM folder_permission"),eq((Object)"10"))).thenReturn(List.of(Map.of("user_id",2L,"permissions_json","{\"view\":true,\"edit\":false}")));
        assertDoesNotThrow(()->access.requireFolderOperation("20",1L,2L,"view"));assertThrows(BusinessException.class,()->access.requireFolderOperation("20",1L,2L,"edit"));
    }
}
