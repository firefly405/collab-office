package com.collaboffice.task.job;

import com.collaboffice.task.client.NotificationClient;
import com.collaboffice.task.entity.TaskTask;
import com.collaboffice.task.service.impl.TaskServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDueReminderJob {

    private final TaskServiceImpl taskService;
    private final NotificationClient notificationClient;

    @Value("${app.internal-token:default-secret-token}")
    private String internalToken;

    // Retained for internal callers; TaskDeadlineReminderJob owns scheduled delivery.
    public void checkDueTasks() {
        List<TaskTask> dueTasks = taskService.getDueSoonTasks();
        if (dueTasks.isEmpty()) {
            log.debug("No tasks due soon");
            return;
        }
        log.info("Found {} tasks due within 2 days", dueTasks.size());
        for (TaskTask task : dueTasks) {
            try {
                Map<String, Object> notify = new LinkedHashMap<>();
                notify.put("type", 3); // Task reminder
                notify.put("title", "任务即将到期");
                notify.put("content", "任务「" + task.getTitle() + "」将于 " + task.getDueDate() + " 到期");
                notify.put("relatedId", task.getTaskId());
                if (task.getAssigneeId() != null) {
                    notify.put("userId", task.getAssigneeId());
                    notificationClient.sendNotificationInternal(internalToken, notify);
                    log.info("Sent due reminder for task: {}", task.getTaskId());
                }
            } catch (Exception e) {
                log.error("Failed to send reminder for task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }
}

