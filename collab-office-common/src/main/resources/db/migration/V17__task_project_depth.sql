ALTER TABLE task_task
  ADD COLUMN column_id BIGINT NULL,
  ADD COLUMN repeat_rule VARCHAR(10) NOT NULL DEFAULT 'none',
  ADD COLUMN repeat_next_id VARCHAR(32) NULL,
  ADD COLUMN reminder_enabled TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN reminder_days INT NOT NULL DEFAULT 1,
  ADD COLUMN reminder_frequency VARCHAR(10) NOT NULL DEFAULT 'daily',
  ADD COLUMN last_reminded_at DATETIME NULL,
  ADD INDEX idx_task_column (column_id, deleted),
  ADD CONSTRAINT chk_task_repeat CHECK (repeat_rule IN ('none','daily','weekly','monthly')),
  ADD CONSTRAINT chk_task_reminder CHECK (reminder_days BETWEEN 0 AND 30 AND reminder_frequency IN ('once','daily','hourly'));

CREATE TABLE task_project_member (
  project_id VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (project_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
