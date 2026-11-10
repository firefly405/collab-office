package com.collaboffice.task.service;
import com.collaboffice.common.security.WorkspaceAccess;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class MeetingTaskTest {
 @Test void existingTaskIsReturnedWithoutCreatingAnother(){
  var jdbc=mock(JdbcTemplate.class);var access=mock(WorkspaceAccess.class);var tasks=mock(TaskService.class);
  when(access.currentSpace()).thenReturn(13L);when(access.requireSpace(13L,true)).thenReturn(13L);
  when(jdbc.queryForList(startsWith("SELECT id,todo_list_json"),eq("123"))).thenReturn(List.of(Map.of("id",1L,"todo_list_json","[{\"id\":\"a\",\"title\":\"Review\",\"spaceId\":13,\"taskId\":\"42\"}]")));
  when(tasks.getProjectList(13L)).thenReturn(List.of(Map.of("projectId","p","status",1)));
  assertEquals(Map.of("a","42"),new MeetingTaskService(jdbc,access,tasks).convert("123",List.of("a"),null));
  verify(tasks,never()).createTask(any(),any(),any());
 }
}
