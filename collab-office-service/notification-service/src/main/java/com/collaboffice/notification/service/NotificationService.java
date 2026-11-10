package com.collaboffice.notification.service;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    List<Map<String, Object>> getNotifications(Long userId);
    void markAsRead(Long notificationId);
    int getUnreadCount(Long userId);
    void deleteNotification(Long notificationId);
    void sendNotification(Long userId, Integer type, String title, String content, String relatedId);
    List<Map<String, Object>> getOfflineNotifications(Long userId, Long lastSeqId);
}
