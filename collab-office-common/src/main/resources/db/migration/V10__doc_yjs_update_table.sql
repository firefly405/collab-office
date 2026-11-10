-- ==========================================
-- V10: 协同文档 Yjs update 持久化表
-- 配合 collaboration-service 改造，将前端 Yjs CRDT 的 update 字节
-- 真正落库，支持新加入客户端拉取历史 state 做初始化同步
-- ==========================================

CREATE TABLE IF NOT EXISTS `doc_yjs_update` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_id` VARCHAR(64) NOT NULL COMMENT '文档ID',
  `client_id` VARCHAR(64) NOT NULL COMMENT 'Yjs clientID（产生该 update 的客户端）',
  `user_id` BIGINT NULL COMMENT '产生该 update 的用户ID',
  `update_b64` MEDIUMTEXT NOT NULL COMMENT 'Yjs update 字节的 Base64 编码',
  `seq` BIGINT NOT NULL COMMENT '文档内单调递增的序列号',
  `kind` VARCHAR(32) NOT NULL DEFAULT 'yjs_update' COMMENT 'update 种类: yjs_update / yjs_state',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '产生时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_seq` (`doc_id`, `seq`),
  KEY `idx_doc_created` (`doc_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档 Yjs update 持久化表';

-- 每个文档的全局 seq 计数器表（避免对单行加锁）
CREATE TABLE IF NOT EXISTS `doc_yjs_seq` (
  `doc_id` VARCHAR(64) NOT NULL COMMENT '文档ID',
  `seq` BIGINT NOT NULL DEFAULT '0' COMMENT '当前已分配的最大 seq',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档 Yjs seq 计数器';
