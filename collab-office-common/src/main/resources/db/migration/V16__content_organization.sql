CREATE TABLE doc_user_pin (
  user_id bigint NOT NULL, doc_id varchar(32) NOT NULL,
  PRIMARY KEY(user_id,doc_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE chat_pinned_message (
  session_id varchar(32) NOT NULL, msg_id varchar(32) NOT NULL,
  pinned_by bigint NOT NULL, pinned_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(session_id,msg_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
