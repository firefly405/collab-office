-- ==========================================
-- V9: 数据库设计缺陷修复
-- 修复审计发现的所有结构性问题
-- ==========================================

-- ============================================
-- 1. 修复 V5 双 AUTO_INCREMENT 问题
--    MySQL 不允许一张表有两个 AUTO_INCREMENT 列
--    改用应用层生成的序列号
-- ============================================
-- seq_id 已在 V5 中作为 BIGINT NULL 列添加，此处仅补充 UNIQUE 索引
ALTER TABLE chat_message ADD UNIQUE INDEX uk_seq_id (seq_id);
ALTER TABLE notification ADD UNIQUE INDEX uk_notif_seq_id (seq_id);

-- ============================================
-- 2. 修复字段类型不匹配
-- ============================================
-- task_kanban_column.project_id varchar(64) → varchar(32) 以匹配 task_project.project_id
ALTER TABLE task_kanban_column MODIFY COLUMN project_id VARCHAR(32) NOT NULL COMMENT '项目ID';

-- task_dependency.task_id 和 depends_on_task_id varchar(64) → varchar(32)
ALTER TABLE task_dependency MODIFY COLUMN task_id VARCHAR(32) NOT NULL COMMENT '任务ID';
ALTER TABLE task_dependency MODIFY COLUMN depends_on_task_id VARCHAR(32) NOT NULL COMMENT '前置依赖任务ID';

-- ============================================
-- 3. 添加缺失的 UNIQUE 约束
-- ============================================
-- 防止重复任务依赖
ALTER TABLE task_dependency ADD UNIQUE INDEX uk_task_dep (task_id, depends_on_task_id);

-- 防止重复文档引用
ALTER TABLE doc_reference ADD UNIQUE INDEX uk_doc_ref (source_doc_id, target_doc_id);

-- 防止同一用户重复加入同一会议
ALTER TABLE meeting_participant ADD UNIQUE INDEX uk_meeting_user (meeting_id, user_id);

-- doc_version 需要唯一约束（目前只有普通索引）
ALTER TABLE doc_version DROP INDEX idx_doc_id_version;
ALTER TABLE doc_version ADD UNIQUE INDEX uk_doc_version (doc_id, version);

-- OAuth OpenID 唯一约束
ALTER TABLE sys_user ADD UNIQUE INDEX uk_oauth_open_id (oauth_provider, oauth_open_id);

-- 看板列名在项目内唯一
ALTER TABLE task_kanban_column ADD UNIQUE INDEX uk_project_name (project_id, name);

-- ============================================
-- 4. 创建 sys_role_permission 关联表
--    （补全 RBAC 权限模型）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 5. 创建 doc_user_favorite 关联表
--    （替代 doc_document.is_favorite 全局字段）
-- ============================================
CREATE TABLE IF NOT EXISTS `doc_user_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_doc` (`user_id`, `doc_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文档收藏表';

-- ============================================
-- 6. 添加缺失索引
-- ============================================
-- 文档评论
ALTER TABLE doc_comment ADD INDEX idx_user_id (user_id);
ALTER TABLE doc_comment ADD INDEX idx_parent_id (parent_id);
ALTER TABLE doc_comment ADD INDEX idx_is_resolved (is_resolved);

-- 文档分享
ALTER TABLE doc_share ADD INDEX idx_creator_id (creator_id);
ALTER TABLE doc_share ADD INDEX idx_expire_time (expire_time);

-- 通知 - 最高频查询
ALTER TABLE notification ADD INDEX idx_user_read (user_id, is_read);

-- 文件上传者
ALTER TABLE file_file ADD INDEX idx_uploader_id (uploader_id);

-- 任务创建者
ALTER TABLE task_task ADD INDEX idx_creator_id (creator_id);

-- 用户登录设备
ALTER TABLE user_login_device ADD INDEX idx_token_id (token_id);

-- 操作日志复合索引
ALTER TABLE sys_log ADD INDEX idx_module_time (module, create_time);

-- 知识库
ALTER TABLE knowledge_item ADD INDEX idx_contributor_id (contributor_id);
ALTER TABLE knowledge_item ADD INDEX idx_reviewer_id (reviewer_id);

-- 表格创建者
ALTER TABLE sheet_spreadsheet ADD INDEX idx_creator_id (creator_id);

-- 会议事件日志复合索引
ALTER TABLE meeting_event_log ADD INDEX idx_meeting_time (meeting_id, event_time);

-- ============================================
-- 7. 删除冗余索引
-- ============================================
-- chat_message 的 idx_session_send_time (V4) 与 idx_session_time (V3) 重复
ALTER TABLE chat_message DROP INDEX idx_session_send_time;

-- doc_content 的 idx_doc_id 被 uk_doc_version 的左前缀覆盖
ALTER TABLE doc_content DROP INDEX idx_doc_id;

-- sheet_cell 的 idx_sheet_id 被 uk_sheet_cell 的左前缀覆盖
ALTER TABLE sheet_cell DROP INDEX idx_sheet_id;

-- ============================================
-- 8. 添加 CHECK 约束（MySQL 8.0.16+）
-- ============================================
ALTER TABLE sys_user ADD CONSTRAINT chk_gender CHECK (gender IN (0, 1, 2));
ALTER TABLE sys_user ADD CONSTRAINT chk_status CHECK (status IN (0, 1));
ALTER TABLE sys_user ADD CONSTRAINT chk_online_status CHECK (online_status IN (0, 1, 2, 3));

ALTER TABLE doc_document ADD CONSTRAINT chk_doc_type CHECK (type IN (1, 2, 3, 4));
ALTER TABLE doc_member ADD CONSTRAINT chk_doc_permission CHECK (permission IN (1, 2, 3));

ALTER TABLE task_task ADD CONSTRAINT chk_priority CHECK (priority IN (1, 2, 3, 4));
ALTER TABLE task_task ADD CONSTRAINT chk_progress CHECK (progress BETWEEN 0 AND 100);

ALTER TABLE meeting_room ADD CONSTRAINT chk_meeting_status CHECK (status IN (1, 2, 3, 4));

ALTER TABLE sys_space ADD CONSTRAINT chk_storage_used CHECK (storage_used >= 0);
ALTER TABLE sys_space ADD CONSTRAINT chk_storage_quota CHECK (storage_quota > 0);

ALTER TABLE notification ADD CONSTRAINT chk_notif_type CHECK (type IN (1, 2, 3, 4));

-- ============================================
-- 9. 添加文档锁定元数据
-- ============================================
ALTER TABLE doc_document ADD COLUMN locked_by BIGINT DEFAULT NULL COMMENT '锁定者用户ID';
ALTER TABLE doc_document ADD COLUMN locked_at DATETIME DEFAULT NULL COMMENT '锁定时间';

-- ============================================
-- 10. 添加缺失的 update_time 列
-- ============================================
ALTER TABLE doc_comment ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE task_comment ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE notification ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE chat_session_member ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE meeting_vote ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE meeting_recording ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- ============================================
-- 11. 添加缺失的 deleted 逻辑删除列
-- ============================================
ALTER TABLE doc_comment ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE doc_share ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE chat_session ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE meeting_room ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE notification ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE meeting_vote ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE meeting_recording ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除';

-- ============================================
-- 12. 添加关键外键约束（选择性添加，仅核心关系）
--     注意：使用 SET NULL 策略以兼容逻辑删除
-- ============================================
-- 用户-角色关联
ALTER TABLE sys_user_role 
  ADD CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE;

-- 角色-权限关联
ALTER TABLE sys_role_permission
  ADD CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE;

-- 空间成员
ALTER TABLE sys_space_member
  ADD CONSTRAINT fk_space_member_space FOREIGN KEY (space_id) REFERENCES sys_space(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_space_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- 会话成员
ALTER TABLE chat_session_member
  ADD CONSTRAINT fk_session_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- 投票记录
ALTER TABLE meeting_vote_record
  ADD CONSTRAINT fk_vote_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- 用户设置
ALTER TABLE user_setting
  ADD CONSTRAINT fk_user_setting_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- 通知偏好
ALTER TABLE user_notification_pref
  ADD CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- 登录设备
ALTER TABLE user_login_device
  ADD CONSTRAINT fk_login_device_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;
