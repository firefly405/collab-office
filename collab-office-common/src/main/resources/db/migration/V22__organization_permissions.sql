ALTER TABLE sys_space_member ADD COLUMN custom_role_id BIGINT NULL, ADD COLUMN dept_id BIGINT NULL, ADD COLUMN expires_at DATETIME NULL;
CREATE TABLE workspace_role (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,space_id BIGINT NOT NULL,name VARCHAR(50) NOT NULL,
 permissions_json JSON NOT NULL,UNIQUE KEY uk_space_role(space_id,name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE folder_permission (
 folder_id VARCHAR(32) NOT NULL,user_id BIGINT NOT NULL,permissions_json JSON NOT NULL,
 PRIMARY KEY(folder_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE workspace_audit (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,space_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,
 action VARCHAR(50) NOT NULL,target VARCHAR(100) NOT NULL,detail_json JSON NULL,
 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,KEY idx_space_audit(space_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
