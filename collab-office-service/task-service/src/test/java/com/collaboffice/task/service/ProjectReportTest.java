package com.collaboffice.task.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.task.controller.ProjectReportController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectReportTest {
    final WorkspaceAccess access=mock(WorkspaceAccess.class);
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final ProjectReportController reports=new ProjectReportController(access,jdbc);
    @Test void memberCannotReview(){when(access.row(anyString(),eq(1L))).thenReturn(Map.of("project_id","100"));doThrow(new BusinessException(403,"Forbidden")).when(access).requireProject("100",true);assertThrows(BusinessException.class,()->reports.review(1L));verifyNoInteractions(jdbc);}
    @Test void outsiderCannotReply(){when(access.row(anyString(),eq(1L))).thenReturn(Map.of("project_id","100"));doThrow(new BusinessException(403,"Forbidden")).when(access).requireProject("100",false);assertThrows(BusinessException.class,()->reports.reply(1L,Map.of("content","reply")));verifyNoInteractions(jdbc);}
    @Test void blankReplyDoesNotWrite(){when(access.row(anyString(),eq(1L))).thenReturn(Map.of("project_id","100"));assertThrows(BusinessException.class,()->reports.reply(1L,Map.of("content","  ")));verifyNoInteractions(jdbc);}
    @Test void memberReplyUsesAuthenticatedAuthor(){when(access.row(anyString(),eq(1L))).thenReturn(Map.of("project_id","100"));when(access.userId()).thenReturn(2L);reports.reply(1L,Map.of("content","Progress"));verify(jdbc).update(contains("INSERT INTO project_report_reply"),eq(1L),eq(2L),eq("Progress"));}
}
