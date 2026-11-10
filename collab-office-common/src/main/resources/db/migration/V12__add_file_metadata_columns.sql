-- Align file metadata storage with the file-service entity and statistics API.
ALTER TABLE `file_file`
  ADD COLUMN `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型' AFTER `file_type`,
  ADD COLUMN `uploader_name` varchar(50) DEFAULT NULL COMMENT '上传者名称' AFTER `uploader_id`,
  ADD KEY `idx_file_mime_type` (`mime_type`);
