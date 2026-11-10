-- ==========================================
-- V2: Document / Sheet / Mindmap / Whiteboard Tables
-- ==========================================

CREATE TABLE IF NOT EXISTS `doc_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档唯一ID',
  `title` varchar(200) NOT NULL COMMENT '文档标题',
  `type` tinyint DEFAULT '1' COMMENT '文档类型 1-文档 2-表格 3-思维导图 4-白板',
  `space_id` bigint DEFAULT NULL COMMENT '所属空间ID',
  `folder_id` bigint DEFAULT NULL COMMENT '所属文件夹ID',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `creator_name` varchar(50) DEFAULT NULL COMMENT '创建者姓名',
  `current_version` bigint DEFAULT '1' COMMENT '当前版本号',
  `word_count` int DEFAULT '0' COMMENT '字数统计',
  `is_public` tinyint DEFAULT '0' COMMENT '是否公开',
  `is_template` tinyint DEFAULT '0' COMMENT '是否模板',
  `is_favorite` tinyint DEFAULT '0' COMMENT '是否收藏',
  `is_locked` tinyint DEFAULT '0' COMMENT '是否锁定',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-回收站 1-正常',
  `last_edit_time` datetime DEFAULT NULL COMMENT '最后编辑时间',
  `last_editor_id` bigint DEFAULT NULL COMMENT '最后编辑者ID',
  `last_editor_name` varchar(50) DEFAULT NULL COMMENT '最后编辑者姓名',
  `edit_count` int DEFAULT '0' COMMENT '编辑次数',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_id` (`doc_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_last_edit_time` (`last_edit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

CREATE TABLE IF NOT EXISTS `doc_content` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档ID',
  `version` bigint NOT NULL COMMENT '版本号',
  `content` longtext COMMENT '文档内容JSON',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容哈希值',
  `delta` longtext COMMENT '增量操作JSON',
  `is_snapshot` tinyint DEFAULT '0' COMMENT '是否快照',
  `editor_id` bigint DEFAULT NULL COMMENT '编辑者ID',
  `editor_name` varchar(50) DEFAULT NULL COMMENT '编辑者姓名',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '版本备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_version` (`doc_id`,`version`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档内容表';

CREATE TABLE IF NOT EXISTS `doc_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档ID',
  `version` bigint NOT NULL COMMENT '版本号',
  `version_name` varchar(100) DEFAULT NULL COMMENT '版本名称',
  `snapshot_data` longtext COMMENT '快照数据',
  `editor_id` bigint DEFAULT NULL COMMENT '编辑者ID',
  `editor_name` varchar(50) DEFAULT NULL COMMENT '编辑者姓名',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '变更摘要',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容哈希',
  `is_snapshot` tinyint DEFAULT '0' COMMENT '是否快照',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_id_version` (`doc_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档版本表';

CREATE TABLE IF NOT EXISTS `doc_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `selection_range_json` varchar(500) DEFAULT NULL COMMENT '选区范围JSON',
  `content` text NOT NULL COMMENT '评论内容',
  `is_resolved` tinyint DEFAULT '0' COMMENT '是否已解决',
  `resolved_time` datetime DEFAULT NULL COMMENT '解决时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档评论表';

CREATE TABLE IF NOT EXISTS `doc_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `permission` tinyint DEFAULT '1' COMMENT '权限 1-查看 2-编辑 3-管理',
  `is_online` tinyint DEFAULT '0' COMMENT '是否在线',
  `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
  `join_time` datetime DEFAULT NULL COMMENT '加入时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_user` (`doc_id`,`user_id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档成员表';

CREATE TABLE IF NOT EXISTS `doc_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` varchar(32) NOT NULL COMMENT '文档ID',
  `share_token` varchar(64) NOT NULL COMMENT '分享令牌',
  `share_password` varchar(10) DEFAULT NULL COMMENT '分享密码',
  `permission` tinyint DEFAULT '1' COMMENT '权限 1-查看 2-编辑 3-评论',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分享表';

CREATE TABLE IF NOT EXISTS `sheet_spreadsheet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` varchar(32) NOT NULL COMMENT '表格ID',
  `title` varchar(200) NOT NULL COMMENT '表格标题',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `folder_id` bigint DEFAULT NULL COMMENT '文件夹ID',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `sheet_count` int DEFAULT '1' COMMENT '工作表数量',
  `row_count` int DEFAULT '1000' COMMENT '行数',
  `col_count` int DEFAULT '26' COMMENT '列数',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `last_edit_time` datetime DEFAULT NULL COMMENT '最后编辑时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_id` (`sheet_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子表格表';

CREATE TABLE IF NOT EXISTS `sheet_sheet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `spreadsheet_id` varchar(32) NOT NULL COMMENT '所属表格ID',
  `sheet_name` varchar(50) DEFAULT 'Sheet1' COMMENT '工作表名',
  `sheet_index` int DEFAULT '0' COMMENT '工作表索引',
  `row_count` int DEFAULT '1000' COMMENT '行数',
  `col_count` int DEFAULT '26' COMMENT '列数',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_spreadsheet_id` (`spreadsheet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作表表';

CREATE TABLE IF NOT EXISTS `sheet_cell` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` varchar(32) NOT NULL COMMENT '所属工作表ID',
  `cell_key` varchar(20) NOT NULL COMMENT '单元格键 A1,B2等',
  `row_index` int NOT NULL COMMENT '行索引',
  `col_index` int NOT NULL COMMENT '列索引',
  `content` text COMMENT '单元格内容',
  `format_json` json DEFAULT NULL COMMENT '格式JSON',
  `formula` varchar(500) DEFAULT NULL COMMENT '公式',
  `result_value` varchar(500) DEFAULT NULL COMMENT '计算结果',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_cell` (`sheet_id`,`cell_key`),
  KEY `idx_sheet_id` (`sheet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单元格表';

CREATE TABLE IF NOT EXISTS `mindmap_map` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `map_id` varchar(32) NOT NULL COMMENT '思维导图ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `layout_type` varchar(20) DEFAULT 'mindmap' COMMENT '布局类型',
  `theme_json` json DEFAULT NULL COMMENT '主题JSON',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `last_edit_time` datetime DEFAULT NULL COMMENT '最后编辑时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_map_id` (`map_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='思维导图表';

CREATE TABLE IF NOT EXISTS `mindmap_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `map_id` varchar(32) NOT NULL COMMENT '所属导图ID',
  `parent_id` varchar(32) DEFAULT NULL COMMENT '父节点ID',
  `node_key` varchar(32) NOT NULL COMMENT '节点键',
  `content` varchar(500) NOT NULL COMMENT '节点内容',
  `style_json` json DEFAULT NULL COMMENT '样式JSON',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `note` text COMMENT '备注',
  `link` varchar(500) DEFAULT NULL COMMENT '链接',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_key` (`node_key`),
  KEY `idx_map_id` (`map_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='思维导图节点表';

CREATE TABLE IF NOT EXISTS `whiteboard_board` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `board_id` varchar(32) NOT NULL COMMENT '白板ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `width` int DEFAULT '1920' COMMENT '画布宽度',
  `height` int DEFAULT '1080' COMMENT '画布高度',
  `background_color` varchar(20) DEFAULT '#FFFFFF' COMMENT '背景色',
  `space_id` bigint DEFAULT NULL COMMENT '空间ID',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `last_edit_time` datetime DEFAULT NULL COMMENT '最后编辑时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_id` (`board_id`),
  KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='白板表';

CREATE TABLE IF NOT EXISTS `whiteboard_element` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `board_id` varchar(32) NOT NULL COMMENT '所属白板ID',
  `element_type` varchar(20) NOT NULL COMMENT '元素类型 pen/rect/circle/line/text/eraser',
  `position_json` json DEFAULT NULL COMMENT '位置JSON',
  `size_json` json DEFAULT NULL COMMENT '尺寸JSON',
  `style_json` json DEFAULT NULL COMMENT '样式JSON',
  `content` text COMMENT '内容(文字元素)',
  `z_index` int DEFAULT '0' COMMENT '层级',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_board_id` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='白板元素表';

CREATE TABLE IF NOT EXISTS `template_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` varchar(32) NOT NULL COMMENT '模板ID',
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `type` tinyint NOT NULL COMMENT '类型 1-文档 2-表格 3-思维导图 4-白板',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `content_json` longtext COMMENT '内容JSON',
  `thumbnail_url` varchar(255) DEFAULT NULL COMMENT '缩略图URL',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `is_official` tinyint DEFAULT '0' COMMENT '是否官方模板',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_id` (`template_id`),
  KEY `idx_type_category` (`type`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板表';
