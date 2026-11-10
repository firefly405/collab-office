CREATE TABLE IF NOT EXISTS `user_notification_pref` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `email_notify` tinyint DEFAULT '1' COMMENT '邮件通知 (0-关闭, 1-开启)',
  `push_notify` tinyint DEFAULT '1' COMMENT '推送通知 (0-关闭, 1-开启)',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知偏好表';
