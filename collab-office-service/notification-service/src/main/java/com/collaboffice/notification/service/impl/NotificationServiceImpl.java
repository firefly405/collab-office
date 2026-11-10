package com.collaboffice.notification.service.impl;

import com.collaboffice.notification.entity.Notification;
import com.collaboffice.notification.mapper.NotificationMapper;
import com.collaboffice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    private static final String NOTIFICATION_PUSH_EXCHANGE = "notification-push-exchange";

    @Override
    public List<Map<String, Object>> getNotifications(Long userId) {
        List<Notification> list = notificationMapper.selectByUserId(userId, null, null, 50);
        return list.stream().map(n -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", n.getId());
            map.put("type", n.getType());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("relatedId", n.getRelatedId());
            map.put("isRead", n.getIsRead() == 1);
            map.put("sendTime", n.getSendTime());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n != null) {
            n.setIsRead(1);
            n.setReadTime(java.time.LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    @Override
    public int getUnreadCount(Long userId) {
        Integer count = notificationMapper.countUnread(userId);
        return count != null ? count : 0;
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationMapper.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void sendNotification(Long userId, Integer type, String title, String content, String relatedId) {
        var prefs=jdbc.queryForList("SELECT preference_json FROM user_setting WHERE user_id=?",String.class,userId);
        String category=switch(type==null?1:type){case 2->"collab";case 3->"task";case 4->"meeting";default->"system";};
        if(!prefs.isEmpty()&&prefs.get(0)!=null){var categories=com.alibaba.fastjson2.JSON.parseObject(prefs.get(0)).getJSONObject("notifications");if(categories!=null&&Boolean.FALSE.equals(categories.getBoolean(category)))return;}
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        n.setSendTime(java.time.LocalDateTime.now());
        notificationMapper.insert(n);

        try {
            Map<String, Object> pushPayload = new LinkedHashMap<>();
            pushPayload.put("id", n.getId());
            pushPayload.put("userId", userId);
            pushPayload.put("type", type);
            pushPayload.put("title", title);
            pushPayload.put("content", content);
            pushPayload.put("relatedId", relatedId);
            pushPayload.put("sendTime", n.getSendTime().toString());
            rabbitTemplate.convertAndSend(NOTIFICATION_PUSH_EXCHANGE, "", pushPayload);
            log.info("Notification published to push exchange: {}", pushPayload);
        } catch (Exception e) {
            log.error("Failed to publish notification push: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getOfflineNotifications(Long userId, Long lastSeqId) {
        List<Notification> list = notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .gt(Notification::getSeqId, lastSeqId)
                        .orderByAsc(Notification::getSeqId)
        );
        return list.stream().map(n -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", n.getId());
            map.put("type", n.getType());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("relatedId", n.getRelatedId());
            map.put("isRead", n.getIsRead() == 1);
            map.put("sendTime", n.getSendTime());
            map.put("seqId", n.getSeqId());
            return map;
        }).collect(Collectors.toList());
    }
}
