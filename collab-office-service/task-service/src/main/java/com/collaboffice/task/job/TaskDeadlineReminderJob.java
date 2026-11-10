package com.collaboffice.task.job;

import com.collaboffice.task.entity.TaskTask;
import com.collaboffice.task.mapper.TaskTaskMapper;
import com.collaboffice.task.service.TaskSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Gap-5 任务截止提醒定时任务
 * 每小时扫描：将截止时间在未来1小时内的任务入队 task.deadline.approaching
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDeadlineReminderJob {

    private final TaskTaskMapper taskMapper;
    private final AmqpTemplate rabbitTemplate;

    private static final String NOTIFICATION_EXCHANGE = "notification-exchange";
    private static final String DEADLINE_ROUTING_KEY = "task.deadline.approaching";

    /**
     * 每小时整点扫描任务截止情况
     * 将【今天到期 + 明天到期（24h内）】且分配了责任人的任务发送提醒通知
     */
    @Scheduled(cron = "0 0 * * * ?")  // 每小时整点触发
    public void checkDeadlineApproachingTasks() {
        log.info("开始扫描即将到期任务...");
        try {
            LocalDateTime now = LocalDateTime.now();

            List<TaskTask> tasks = taskMapper.selectList(null);
            List<TaskTask> approaching = new ArrayList<>();

            for (TaskTask task : tasks) {
                if (TaskSchedule.shouldRemind(task,now)) {
                    approaching.add(task);
                }
            }

            log.info("发现 {} 个即将到期任务，开始入队", approaching.size());

            for (TaskTask task : approaching) {
                try {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("userId", task.getAssigneeId());
                    payload.put("taskId", task.getTaskId());
                    payload.put("taskTitle", task.getTitle());
                    payload.put("dueDate", task.getDueDate().toString());
                    payload.put("reminderTime", LocalDateTime.now().toString());

                    rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, DEADLINE_ROUTING_KEY, payload);
                    task.setLastRemindedAt(now);taskMapper.updateById(task);
                    log.info("任务截止提醒已入队: taskId={}, assignee={}", task.getTaskId(), task.getAssigneeId());
                } catch (Exception e) {
                    log.error("任务截止提醒入队失败: taskId={}, error={}", task.getTaskId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("扫描即将到期任务时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天上午9点发送当日到期汇总提醒
     */
    public void checkTodayDeadlineTasks() {
        log.info("开始扫描今日到期任务...");
        try {
            LocalDate today = LocalDate.now();
            List<TaskTask> tasks = taskMapper.selectList(null);

            for (TaskTask task : tasks) {
                if (task.getDueDate() == null) continue;
                if (task.getStatus() != null && task.getStatus() >= 2) continue;
                if (task.getAssigneeId() == null) continue;
                if (!task.getDueDate().equals(today)) continue;

                try {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("userId", task.getAssigneeId());
                    payload.put("taskId", task.getTaskId());
                    payload.put("taskTitle", task.getTitle());
                    payload.put("dueDate", task.getDueDate().toString());
                    payload.put("reminderType", "TODAY");

                    rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, DEADLINE_ROUTING_KEY, payload);
                } catch (Exception e) {
                    log.error("今日到期提醒入队失败: taskId={}", task.getTaskId(), e);
                }
            }
        } catch (Exception e) {
            log.error("扫描今日到期任务时发生异常: {}", e.getMessage(), e);
        }
    }
}
