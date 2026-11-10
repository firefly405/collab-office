-- ==========================================
-- V3: Chat / Meeting / Task / File / Knowledge / Notification Tables
-- ==========================================

CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `session_type` tinyint DEFAULT '1' COMMENT '会话类型 1-单聊 2-群聊',
  `session_name` varchar(100) DEFAULT NULL COMMENT '会话名称',
  `session_avatar` varchar(255) DEFAULT NULL COMMENT '会话头像',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `last_msg_content` varchar(500) DEFAULT NULL COMMENT '最后消息内容',
  `last_msg_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `last_msg_sender_id` bigint DEFAULT NULL COMMENT '最后消息发送者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `msg_id` varchar(32) NOT NULL COMMENT '消息ID',
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(50) DEFAULT NULL COMMENT '发送者姓名',
  `sender_avatar` varchar(255) DEFAULT NULL COMMENT '发送者头像',
  `msg_type` tinyint DEFAULT '1' COMMENT '消息类型 1-文本 2-图片 3-文件 4-语音 5-视频 6-位置 7-卡片',
  `content` text COMMENT '消息内容',
  `extra` json DEFAULT NULL COMMENT '扩展信息JSON',
  `reply_to_id` varchar(32) DEFAULT NULL COMMENT '回复消息ID',
  `is_edited` tinyint DEFAULT '0' COMMENT '是否编辑过',
  `is_recalled` tinyint DEFAULT '0' COMMENT '是否撤回',
  `recall_time` datetime DEFAULT NULL COMMENT '撤回时间',
  `mention_users` varchar(500) DEFAULT NULL COMMENT '@用户ID列表JSON',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-删除 1-正常',
  `send_time` datetime NOT NULL COMMENT '发送时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_session_time` (`session_id`,`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS `chat_session_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(10) DEFAULT 'MEMBER' COMMENT '角色 OWNER/ADMIN/MEMBER',
  `nickname` varchar(50) DEFAULT NULL COMMENT '群昵称',
  `is_muted` tinyint DEFAULT '0' COMMENT '是否免打扰',
  `unread_count` int DEFAULT '0' COMMENT '未读数',
  `joined_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_user` (`session_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员表';

CREATE TABLE IF NOT EXISTS `meeting_room` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` varchar(32) NOT NULL COMMENT '会议房间ID',
  `title` varchar(200) NOT NULL COMMENT '会议标题',
  `agenda` text COMMENT '会议议程',
  `host_id` bigint NOT NULL COMMENT '主持人ID',
  `host_name` varchar(50) DEFAULT NULL COMMENT '主持人姓名',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` int DEFAULT '0' COMMENT '时长(秒)',
  `status` tinyint DEFAULT '1' COMMENT '状态 1-待开始 2-进行中 3-已结束 4-已取消',
  `record_url` varchar(500) DEFAULT NULL COMMENT '录制URL',
  `password` varchar(20) DEFAULT NULL COMMENT '入会密码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_id` (`room_id`),
  KEY `idx_host_id` (`host_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议房间表';

CREATE TABLE IF NOT EXISTS `meeting_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` varchar(32) NOT NULL COMMENT '会议ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `join_time` datetime DEFAULT NULL COMMENT '加入时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离开时间',
  `duration` int DEFAULT '0' COMMENT '参与时长(秒)',
  `role_type` varchar(20) DEFAULT 'PARTICIPANT' COMMENT '角色 HOST/SPEAKER/PARTICIPANT',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议参与者表';

CREATE TABLE IF NOT EXISTS `meeting_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` varchar(32) NOT NULL COMMENT '会议ID',
  `minute_content` text COMMENT '会议纪要内容',
  `todo_list_json` json DEFAULT NULL COMMENT '待办列表JSON',
  `recording_url` varchar(500) DEFAULT NULL COMMENT '录制URL',
  `duration` int DEFAULT '0' COMMENT '录制时长(秒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议记录表';

CREATE TABLE IF NOT EXISTS `task_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(32) NOT NULL COMMENT '任务ID',
  `project_id` varchar(32) DEFAULT NULL COMMENT '项目ID',
  `parent_id` varchar(32) DEFAULT NULL COMMENT '父任务ID',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待办 1-进行中 2-已完成 3-已取消',
  `priority` tinyint DEFAULT '2' COMMENT '优先级 1-紧急 2-高 3-中 4-低',
  `assignee_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `assignee_name` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `creator_name` varchar(50) DEFAULT NULL COMMENT '创建者姓名',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `due_date` date DEFAULT NULL COMMENT '截止日期',
  `completed_date` datetime DEFAULT NULL COMMENT '完成日期',
  `estimated_hours` decimal(5,1) DEFAULT NULL COMMENT '预估工时',
  `actual_hours` decimal(5,1) DEFAULT NULL COMMENT '实际工时',
  `progress` tinyint DEFAULT '0' COMMENT '进度 0-100',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签JSON',
  `attachments` json DEFAULT NULL COMMENT '附件JSON',
  `checklist` json DEFAULT NULL COMMENT '检查清单JSON',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_archived` tinyint DEFAULT '0' COMMENT '是否归档',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

CREATE TABLE IF NOT EXISTS `task_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(32) NOT NULL COMMENT '项目ID',
  `name` varchar(200) NOT NULL COMMENT '项目名称',
  `description` text COMMENT '项目描述',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `progress` tinyint DEFAULT '0' COMMENT '进度 0-100',
  `milestones_json` json DEFAULT NULL COMMENT '里程碑JSON',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '截止日期',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_id` (`project_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

CREATE TABLE IF NOT EXISTS `task_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(32) NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `content` text NOT NULL COMMENT '评论内容',
  `mention_users_json` json DEFAULT NULL COMMENT '@用户列表JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务评论表';

CREATE TABLE IF NOT EXISTS `file_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` varchar(32) NOT NULL COMMENT '文件ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件URL',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图URL',
  `folder_id` varchar(32) DEFAULT NULL COMMENT '所属文件夹ID',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `uploader_id` bigint NOT NULL COMMENT '上传者ID',
  `version` int DEFAULT '1' COMMENT '版本号',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-回收站 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_id` (`file_id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

CREATE TABLE IF NOT EXISTS `file_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `folder_id` varchar(32) NOT NULL COMMENT '文件夹ID',
  `parent_id` varchar(32) DEFAULT NULL COMMENT '父文件夹ID',
  `folder_name` varchar(100) NOT NULL COMMENT '文件夹名',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_folder_id` (`folder_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件夹表';

CREATE TABLE IF NOT EXISTS `knowledge_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_id` varchar(32) NOT NULL COMMENT '知识条目ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` longtext COMMENT '内容',
  `tags_json` json DEFAULT NULL COMMENT '标签JSON',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-草稿 1-已发布',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核者ID',
  `contributor_id` bigint DEFAULT NULL COMMENT '贡献者ID',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_id` (`knowledge_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识条目表';

CREATE TABLE IF NOT EXISTS `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` tinyint DEFAULT '1' COMMENT '类型 1-系统 2-协作 3-任务 4-会议',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text COMMENT '通知内容',
  `related_id` varchar(100) DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `send_time` datetime NOT NULL COMMENT '发送时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
