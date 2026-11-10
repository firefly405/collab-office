package com.collaboffice.search.service;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SearchQueryTest {
    @Test void validatesDatesScopesAndLiteralWildcards(){
        assertThrows(BusinessException.class,()->SearchQueryService.validate("x","bogus",null,null,null));
        assertThrows(BusinessException.class,()->SearchQueryService.validate("x","",LocalDate.of(2026,2,2),LocalDate.of(2026,2,1),null));
        assertEquals("%a!%!_!!%",SearchQueryService.like("a%_!"));
        var args=new ArrayList<Object>();String sql=SearchQueryService.filters("time","creator","tags",LocalDate.of(2026,1,1),LocalDate.of(2026,1,2),9L,"qa",args);assertTrue(sql.contains("time<?"));assertEquals(LocalDate.of(2026,1,3).atStartOfDay(),args.get(1));assertEquals(9L,args.get(2));
    }
    @Test void documentPermissionIsCheckedAfterRetrieval(){
        var engine=mock(SearchService.class);var access=mock(WorkspaceAccess.class);var jdbc=mock(JdbcTemplate.class);when(access.currentSpace()).thenReturn(15L);when(engine.globalSearch("plan","document")).thenReturn(List.of());
        var a=new HashMap<String,Object>(Map.of("id","allowed","documentType",1,"title","Plan"));var b=new HashMap<String,Object>(Map.of("id","private","documentType",1));when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(a,b));doThrow(new BusinessException(403,"denied")).when(access).requireDocument("private",false);
        var found=new SearchQueryService(engine,access,jdbc).search("plan","document",null,null,null,null);assertEquals(1,found.size());assertEquals("allowed",found.get(0).get("id"));verify(access).requireDocument("allowed",false);
    }
    @Test void messageQueryRequiresMembershipCurrentTeamAndNonRecalledState(){
        var access=mock(WorkspaceAccess.class);when(access.currentSpace()).thenReturn(15L);when(access.userId()).thenReturn(7L);var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(anyString(),any(Object[].class))).thenAnswer(inv->{String sql=inv.getArgument(0);assertTrue(sql.contains("sm.user_id=?"));assertTrue(sql.contains("s.space_id=?"));assertTrue(sql.contains("COALESCE(m.is_recalled,0)=0"));assertTrue(sql.contains("s.status=1"));return List.of();});new SearchQueryService(null,access,jdbc).search("message","message",null,null,null,null);verify(jdbc).queryForList(anyString(),eq(7L),eq(15L),eq("%message%"));
    }
}
