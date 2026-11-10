-- ==========================================
-- V6: SheetSheet FK fix + chat_message source columns
-- ==========================================

-- Fix SheetSheet.spreadsheet_id from bigint to varchar(32) matching sheet_spreadsheet.sheet_id
ALTER TABLE sheet_sheet MODIFY COLUMN spreadsheet_id VARCHAR(32) NOT NULL COMMENT '所属表格业务ID';

-- Add source tracking to chat_message for doc-sidebar @mentions
ALTER TABLE chat_message ADD COLUMN source_type VARCHAR(20) DEFAULT NULL COMMENT '来源类型 DOC/SHEET/TASK';
ALTER TABLE chat_message ADD COLUMN source_id VARCHAR(32) DEFAULT NULL COMMENT '来源ID';

-- Add index for source-based aggregation
ALTER TABLE chat_message ADD INDEX idx_source (source_type, source_id);

-- Ensure meeting_event_log table (created in V5) has proper indexes
-- (table already exists from V5, this is a safety check)
-- ALTER TABLE meeting_event_log ADD INDEX IF NOT EXISTS idx_meeting_time (meeting_id, event_time);
