package com.collaboffice.notification.listener;

import com.collaboffice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Gap-5 业务事件 RabbitMQ Consumer
 * 监听：任务截止提醒 / @提及 / 会议即将开始
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEventListener {

    private final NotificationService notificationService;

    /**
     * 监听任务截止提醒（task-service 每小时定时扫描入队）
     * 队列：task.deadline.approaching
     */
    @RabbitListener(queues = "task.deadline.approaching")
    public void onTaskDeadlineApproaching(Map<String, Object> payload) {
        log.info("收到任务截止提醒事件: {}", payload);
        try {
            Long userId = extractLong(payload.get("userId"));
            if (userId == null) {
                log.warn("任务截止提醒缺少 userId，丢弃: {}", payload);
                return;
            }
            String taskTitle = (String) payload.getOrDefault("taskTitle", "未知任务");
            String dueDate = (String) payload.getOrDefault("dueDate", "");
            String taskId = (String) payload.getOrDefault("taskId", "");

            notificationService.sendNotification(
                    userId,
                    3, // 3 = 任务提醒
                    "任务即将到期",
                    String.format("任务「%s」将于 %s 到期，请及时处理", taskTitle, dueDate),
                    taskId
            );
            log.info("任务截止提醒已发送给用户 {}: 任务={}", userId, taskTitle);
        } catch (Exception e) {
            log.error("处理任务截止提醒事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听 @提及通知（message-service 发送消息时检测到 @ 入队）
     * 队列：message.mention
     */
    @RabbitListener(queues = "message.mention")
    public void onMessageMention(Map<String, Object> payload) {
        log.info("收到 @提及通知事件: {}", payload);
        try {
            Long userId = extractLong(payload.get("userId"));
            if (userId == null) {
                log.warn("@提及通知缺少 userId，丢弃: {}", payload);
                return;
            }
            String title = (String) payload.getOrDefault("title", "提及通知");
            String content = (String) payload.getOrDefault("content", "您被 @ 了");
            String relatedId = (String) payload.getOrDefault("relatedId", "");

            notificationService.sendNotification(userId, 1, title, content, relatedId);
            log.info("@提及通知已发送给用户 {}", userId);
        } catch (Exception e) {
            log.error("处理 @提及通知事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听会议即将开始通知（meeting-service 会议开始前5分钟入队）
     * 队列：meeting.starting.soon
     */
    @RabbitListener(queues = "meeting.starting.soon")
    public void onMeetingStartingSoon(Map<String, Object> payload) {
        log.info("收到会议即将开始通知事件: {}", payload);
        try {
            // 向所有参会人发送通知
            Object userIdsObj = payload.get("userIds");
            String meetingTitle = (String) payload.getOrDefault("meetingTitle", "未知会议");
            String meetingId = (String) payload.getOrDefault("meetingId", "");
            String startTime = (String) payload.getOrDefault("startTime", "");

            if (userIdsObj instanceof Iterable<?>) {
                for (Object uid : (Iterable<?>) userIdsObj) {
                    Long userId = extractLong(uid);
                    if (userId != null) {
                        notificationService.sendNotification(
                                userId,
                                4, // 4 = 会议提醒
                                "会议即将开始",
                                String.format("会议「%s」将于 %s 开始，请准备参会", meetingTitle, startTime),
                                meetingId
                        );
                    }
                }
            } else if (userIdsObj != null) {
                Long userId = extractLong(userIdsObj);
                if (userId != null) {
                    notificationService.sendNotification(
                            userId,
                            4,
                            "会议即将开始",
                            String.format("会议「%s」将于 %s 开始", meetingTitle, startTime),
                            meetingId
                    );
                }
            }
            log.info("会议即将开始通知已发送: meetingId={}", meetingId);
        } catch (Exception e) {
            log.error("处理会议即将开始通知事件失败: {}", e.getMessage(), e);
        }
    }

    private Long extractLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }
}
