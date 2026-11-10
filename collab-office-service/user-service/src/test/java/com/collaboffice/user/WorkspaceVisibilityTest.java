package com.collaboffice.user;

import com.collaboffice.user.service.WorkspaceDataService;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceVisibilityTest {
 @Test void hiddenFolderFilesDoNotContributeToStatistics(){
  var jdbc=mock(JdbcTemplate.class);var access=mock(WorkspaceAccess.class);
  when(access.currentSpace()).thenReturn(17L);when(access.userId()).thenReturn(4L);
  when(jdbc.queryForList(contains("FROM file_file"),any(Object[].class))).thenReturn(List.of(Map.of("folder_id","restricted","file_size",99L)));
  doThrow(new BusinessException(403,"denied")).when(access).requireFolderOperation("restricted",17L,4L,"view");
  var result=new WorkspaceDataService(jdbc,access,null,"demo").statistics(false);
  assertEquals(0,result.get("files"));assertEquals(0L,result.get("bytes"));
 }
}
