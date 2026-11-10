ALTER TABLE template_template ADD COLUMN status tinyint NOT NULL DEFAULT 1,
  ADD COLUMN space_id bigint DEFAULT NULL;
ALTER TABLE knowledge_item ADD COLUMN category_id bigint DEFAULT NULL,
  ADD COLUMN contributor_name varchar(100) DEFAULT NULL,
  ADD COLUMN star_count int NOT NULL DEFAULT 0;
CREATE TABLE knowledge_category (
  id bigint PRIMARY KEY AUTO_INCREMENT, space_id bigint NOT NULL,
  name varchar(100) NOT NULL, UNIQUE KEY uk_space_name(space_id,name)
);
CREATE TABLE knowledge_favorite (
  knowledge_id varchar(32) NOT NULL, user_id bigint NOT NULL,
  PRIMARY KEY(knowledge_id,user_id)
);
ALTER TABLE chat_session ADD COLUMN space_id bigint DEFAULT NULL,
  ADD COLUMN announcement text DEFAULT NULL;
ALTER TABLE chat_session_member ADD COLUMN is_pinned tinyint NOT NULL DEFAULT 0,
  ADD COLUMN draft text DEFAULT NULL, ADD COLUMN group_name varchar(50) DEFAULT NULL,
  ADD COLUMN last_read_time datetime DEFAULT NULL;
