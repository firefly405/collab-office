package com.collaboffice.task.service;

import com.collaboffice.task.entity.*;
import com.collaboffice.task.mapper.*;
import com.collaboffice.task.service.impl.TaskServiceImpl;
import com.collaboffice.common.security.WorkspaceAccess;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.LocalDate;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TaskRecurrenceTest {
    @Mock TaskTaskMapper tasks;
    @Mock TaskProjectMapper projects;
    @Mock TaskCommentMapper comments;
    @Mock WorkspaceAccess access;
    @Mock JdbcTemplate jdbc;
    @Mock RabbitTemplate rabbit;
    @InjectMocks TaskServiceImpl service;
    @BeforeEach void mapping() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),""),TaskTask.class);
    }
    @Test void repeatedCompletionCreatesOnlyOneNextOccurrence() {
        var task=new TaskTask();task.setTaskId("100");task.setProjectId("200");task.setTitle("Daily review");
        task.setCreatorId(1L);task.setStatus(0);task.setProgress(0);task.setPriority(3);task.setRepeatRule("daily");task.setDueDate(LocalDate.now());
        var project=new TaskProject();project.setProjectId("200");project.setSpaceId(15L);project.setStatus(1);
        when(tasks.selectOne(any())).thenReturn(task);when(projects.selectOne(any())).thenReturn(project);
        when(jdbc.queryForObject(contains("task_dependency"),eq(Integer.class),eq("100"))).thenReturn(0);
        service.updateTaskStatus("100",2);String next=task.getRepeatNextId();
        assertNotNull(next);service.updateTaskStatus("100",2);
        ArgumentCaptor<TaskTask> created=ArgumentCaptor.forClass(TaskTask.class);verify(tasks,times(1)).insert(created.capture());
        assertEquals(next,created.getValue().getTaskId());assertEquals(LocalDate.now().plusDays(1),created.getValue().getDueDate());
        assertEquals(0,created.getValue().getStatus());assertEquals(100,task.getProgress());
    }
    @Test void archivedProjectRejectsTaskUpdates() {
        var task=new TaskTask();task.setTaskId("100");task.setProjectId("200");
        var project=new TaskProject();project.setStatus(2);
        when(tasks.selectOne(any())).thenReturn(task);when(projects.selectOne(any())).thenReturn(project);
        assertThrows(com.collaboffice.common.exception.BusinessException.class,()->service.updateTask("100",Map.of("title","Changed")));
        verify(tasks,never()).updateById(any(TaskTask.class));
    }
}
