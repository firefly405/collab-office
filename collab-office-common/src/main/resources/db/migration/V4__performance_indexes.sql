-- ==========================================
-- V4: Performance Indexes & Soft-Delete Unification
-- ==========================================

-- Critical composite indexes for common queries
ALTER TABLE doc_member ADD INDEX idx_doc_permission (doc_id, permission);
ALTER TABLE doc_content ADD INDEX idx_doc_version_desc (doc_id, version DESC);
ALTER TABLE sheet_cell ADD INDEX idx_sheet_row_col (sheet_id, row_index, col_index);
ALTER TABLE task_task ADD INDEX idx_project_status (project_id, status);
ALTER TABLE chat_message ADD INDEX idx_session_send_time (session_id, send_time);

-- Unify soft-delete field for doc_document (was using status=0)
-- Note: existing logic uses 'status' field; this migration documents the intent
-- ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS deleted TINYINT DEFAULT 0 AFTER status;
