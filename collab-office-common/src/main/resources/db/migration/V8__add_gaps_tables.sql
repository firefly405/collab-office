-- ==========================================
-- V8: Add missing tables and fields for Gaps implementation
-- ==========================================

-- 1. SysUser MFA and OAuth fields
ALTER TABLE `sys_user` ADD COLUMN `oauth_provider` varchar(50) DEFAULT NULL COMMENT 'OAuth 三方登录提供商 (wechat / wecom)';
ALTER TABLE `sys_user` ADD COLUMN `oauth_open_id` varchar(100) DEFAULT NULL COMMENT 'OAuth 三方登录 OpenID';
ALTER TABLE `sys_user` ADD COLUMN `mfa_secret` varchar(50) DEFAULT NULL COMMENT 'MFA TOTP 密钥';
ALTER TABLE `sys_user` ADD COLUMN `mfa_enabled` tinyint DEFAULT '0' COMMENT 'MFA 是否启用：0=否 1=是';

-- 2. Kanban columns table
CREATE TABLE IF NOT EXISTS `task_kanban_column` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `name` varchar(100) NOT NULL COMMENT '列名称',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `wip_limit` int DEFAULT '0' COMMENT 'WIP限制',
  `color` varchar(20) DEFAULT NULL COMMENT '列颜色',
  `is_done_column` tinyint DEFAULT '0' COMMENT '是否为完成列',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板列定义表';

-- 3. Task dependency table
CREATE TABLE IF NOT EXISTS `task_dependency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `depends_on_task_id` varchar(64) NOT NULL COMMENT '前置依赖任务ID',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_depends_on` (`depends_on_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖关系表';

-- 4. Meeting Interaction: Vote table
CREATE TABLE IF NOT EXISTS `meeting_vote` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vote_id` varchar(32) NOT NULL COMMENT '投票ID',
  `meeting_id` varchar(32) NOT NULL COMMENT '会议ID',
  `title` varchar(200) NOT NULL COMMENT '投票标题',
  `description` varchar(500) DEFAULT NULL COMMENT '投票描述',
  `options` text COMMENT '选项JSON',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-进行中 1-已结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vote_id` (`vote_id`),
  KEY `idx_meeting_id` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议投票表';

-- 5. Meeting Interaction: Vote Record table
CREATE TABLE IF NOT EXISTS `meeting_vote_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vote_id` varchar(32) NOT NULL COMMENT '投票ID',
  `meeting_id` varchar(32) NOT NULL COMMENT '会议ID',
  `user_id` bigint NOT NULL COMMENT '投票人用户ID',
  `selected_options` varchar(500) DEFAULT NULL COMMENT '选择的选项索引JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vote_user` (`vote_id`, `user_id`),
  KEY `idx_meeting_id` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议投票记录表';

-- 6. Meeting Interaction: Recording table
CREATE TABLE IF NOT EXISTS `meeting_recording` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recording_id` varchar(32) NOT NULL COMMENT '录制ID',
  `meeting_id` varchar(32) NOT NULL COMMENT '会议ID',
  `title` varchar(200) DEFAULT NULL COMMENT '录制标题',
  `minio_object_key` varchar(500) DEFAULT NULL COMMENT 'MinIO中的存储Key',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小',
  `duration` int DEFAULT '0' COMMENT '录制时长(秒)',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-录制中 1-已完成 2-失败',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传者用户ID',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recording_id` (`recording_id`),
  KEY `idx_meeting_id` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议录制表';

-- 7. Document reference table
CREATE TABLE IF NOT EXISTS `doc_reference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_doc_id` varchar(32) NOT NULL COMMENT '源文档ID',
  `target_doc_id` varchar(32) NOT NULL COMMENT '目标文档(被引用)ID',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_doc` (`source_doc_id`),
  KEY `idx_target_doc` (`target_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档双向引用关联表';
