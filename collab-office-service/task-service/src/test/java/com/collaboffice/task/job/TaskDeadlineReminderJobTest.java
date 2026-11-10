package com.collaboffice.task.job;

import com.collaboffice.task.entity.TaskTask;
import com.collaboffice.task.mapper.TaskTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskDeadlineReminderJob 单元测试")
class TaskDeadlineReminderJobTest {

    @Mock
    private TaskTaskMapper taskMapper;

    @Mock
    private AmqpTemplate rabbitTemplate;

    @InjectMocks
    private TaskDeadlineReminderJob reminderJob;

    private TaskTask activeTask;
    private TaskTask completedTask;

    @BeforeEach
    void setUp() {
        activeTask = new TaskTask();
        activeTask.setId(1L);
        activeTask.setTaskId("task_active");
        activeTask.setTitle("紧急开发任务");
        activeTask.setStatus(1); // 进行中
        activeTask.setAssigneeId(2001L);
        activeTask.setDueDate(LocalDate.now());

        completedTask = new TaskTask();
        completedTask.setId(2L);
        completedTask.setTaskId("task_done");
        completedTask.setTitle("已归档任务");
        completedTask.setStatus(2); // 已完成
        completedTask.setAssigneeId(2001L);
        completedTask.setDueDate(LocalDate.now());
    }

    @Test
    @DisplayName("测试即将到期任务扫描并入队")
    void testCheckDeadlineApproachingTasks() {
        Mockito.when(taskMapper.selectList(null)).thenReturn(List.of(activeTask, completedTask));
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(eq("notification-exchange"), eq("task.deadline.approaching"), any(Map.class));

        reminderJob.checkDeadlineApproachingTasks();

        // 仅对 activeTask 发送消息，completedTask 被过滤
        Mockito.verify(rabbitTemplate, Mockito.times(1))
                .convertAndSend(eq("notification-exchange"), eq("task.deadline.approaching"), any(Map.class));
    }

    @Test
    @DisplayName("测试今日到期提醒任务并入队")
    void testCheckTodayDeadlineTasks() {
        Mockito.when(taskMapper.selectList(null)).thenReturn(List.of(activeTask, completedTask));
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(eq("notification-exchange"), eq("task.deadline.approaching"), any(Map.class));

        reminderJob.checkTodayDeadlineTasks();

        // 仅对 activeTask 触发
        Mockito.verify(rabbitTemplate, Mockito.times(1))
                .convertAndSend(eq("notification-exchange"), eq("task.deadline.approaching"), any(Map.class));
    }
}
