-- Chat sessions support soft deletion in the message service.
ALTER TABLE `chat_session`
  ADD COLUMN `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-删除 1-正常' AFTER `last_msg_sender_id`,
  ADD KEY `idx_chat_session_status` (`status`);
