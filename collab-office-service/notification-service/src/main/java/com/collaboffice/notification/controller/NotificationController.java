package com.collaboffice.notification.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.collaboffice.common.security.WorkspaceAccess access;

    @GetMapping("/list")
    public CommonResult<List<Map<String, Object>>> list(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(notificationService.getNotifications(userId));
    }

    @PutMapping("/{id}/read")
    public CommonResult<Void> markAsRead(@PathVariable Long id) {
        requireOwner(id);
        notificationService.markAsRead(id);
        return CommonResult.success(null);
    }

    @GetMapping("/unread-count")
    public CommonResult<Integer> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read-all")
    public CommonResult<Void> markAllRead() {
        jdbc.update("UPDATE notification SET is_read=1,read_time=NOW() WHERE user_id=? AND is_read=0",access.userId());
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        requireOwner(id);
        notificationService.deleteNotification(id);
        return CommonResult.success(null);
    }

    private void requireOwner(Long id) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id=? AND user_id=?",Integer.class,id,access.userId())==0) throw new com.collaboffice.common.exception.BusinessException(403,"无权访问此通知");
    }

    @PostMapping("/internal/send")
    public CommonResult<Void> sendNotificationInternal(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer type = params.get("type") != null ? Integer.valueOf(params.get("type").toString()) : 1;
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String relatedId = (String) params.get("relatedId");
        notificationService.sendNotification(userId, type, title, content, relatedId);
        return CommonResult.success(null);
    }

    @GetMapping("/internal/offline")
    public CommonResult<List<Map<String, Object>>> getOfflineNotifications(
            @RequestParam Long userId,
            @RequestParam Long lastSeqId) {
        return CommonResult.success(notificationService.getOfflineNotifications(userId, lastSeqId));
    }
}
