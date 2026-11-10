ALTER TABLE user_setting ADD COLUMN preference_json JSON NULL;
CREATE TABLE user_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL, space_id BIGINT NOT NULL,
  kind VARCHAR(10) NOT NULL, resource_id VARCHAR(32) NOT NULL,
  frequency VARCHAR(10) NOT NULL DEFAULT 'realtime',
  last_signature VARCHAR(64) NOT NULL, pending TINYINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_subscription(user_id,space_id,kind,resource_id),
  CHECK (kind IN ('document','project','task')),
  CHECK (frequency IN ('realtime','daily'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
