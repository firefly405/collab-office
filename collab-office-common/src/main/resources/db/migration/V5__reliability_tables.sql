-- ==========================================
-- V5: Message Reliability — seq_id + MSG_SYNC support
-- ==========================================

-- Add seq_id to chat_message for offline message sync
ALTER TABLE chat_message ADD COLUMN seq_id BIGINT NULL AFTER id;

-- Add seq_id to notification for offline notification sync
ALTER TABLE notification ADD COLUMN seq_id BIGINT NULL AFTER id;

-- Create user_login_device table for device management (M1-1)
CREATE TABLE IF NOT EXISTS `user_login_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
  `device_type` varchar(20) DEFAULT 'WEB' COMMENT '设备类型 WEB/ANDROID/IOS',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `token_id` varchar(100) DEFAULT NULL COMMENT '关联Token标识',
  `is_current` tinyint DEFAULT '0' COMMENT '是否当前设备',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录设备表';

-- Create user_setting table for personal settings (M10-1)
CREATE TABLE IF NOT EXISTS `user_setting` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `language` varchar(10) DEFAULT 'zh-CN' COMMENT '语言',
  `timezone` varchar(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `email_notify` tinyint DEFAULT '1' COMMENT '邮件通知',
  `push_notify` tinyint DEFAULT '1' COMMENT '推送通知',
  `privacy_level` tinyint DEFAULT '1' COMMENT '隐私级别 1-公开 2-仅成员 3-私密',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- Create mindmap_snapshot table for version history (M4-1)
CREATE TABLE IF NOT EXISTS `mindmap_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mindmap_id` varchar(32) NOT NULL COMMENT '思维导图ID',
  `snapshot_data` longtext COMMENT '快照数据JSON',
  `created_by` bigint DEFAULT NULL COMMENT '创建者',
  `label` varchar(100) DEFAULT NULL COMMENT '快照标签',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mindmap_id` (`mindmap_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='思维导图快照表';

-- Create whiteboard_template table (M4-2)
CREATE TABLE IF NOT EXISTS `whiteboard_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) NOT NULL COMMENT '模版名称',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图URL',
  `template_data` longtext COMMENT '模版数据JSON',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `is_system` tinyint DEFAULT '0' COMMENT '是否系统预置',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='白板模版表';

-- Insert preset whiteboard templates
INSERT INTO whiteboard_template (name, category, is_system, template_data) VALUES
('空白画布', '基础', 1, '{"elements":[]}'),
('流程图框架', '流程', 1, '{"elements":[{"type":"rect","position":"{\"x\":100,\"y\":50}","size":"{\"w\":120,\"h\":60}","content":"开始"}]}'),
('头脑风暴框架', '创意', 1, '{"elements":[{"type":"circle","position":"{\"x\":200,\"y\":150}","size":"{\"w\":100,\"h\":100}","content":"中心主题"}]}');

-- Create sheet_chart table (M3-2)
CREATE TABLE IF NOT EXISTS `sheet_chart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` varchar(32) NOT NULL COMMENT '工作表ID',
  `chart_type` varchar(30) NOT NULL COMMENT '图表类型 bar/line/pie/scatter',
  `config_json` json COMMENT '图表配置JSON',
  `position_json` json COMMENT '位置JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图表元数据表';

-- Create meeting_event_log table (M6-1)
CREATE TABLE IF NOT EXISTS `meeting_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` varchar(32) NOT NULL COMMENT '会议ID',
  `operator_id` bigint NOT NULL COMMENT '操作者ID',
  `event_type` varchar(30) NOT NULL COMMENT '事件类型 MUTE_ALL/MUTE_USER/KICK_USER/GRANT_HOST',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `event_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议事件日志表';
