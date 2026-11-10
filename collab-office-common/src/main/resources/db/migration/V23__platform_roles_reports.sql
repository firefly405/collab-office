INSERT IGNORE INTO sys_role(role_name,role_code,role_sort) VALUES
('系统管理员','admin',1),('项目管理员','project_admin',2),('普通用户','member',3);

-- Existing team collaborators retain project visibility; subsequent membership is explicit.
INSERT IGNORE INTO task_project_member(project_id,user_id)
SELECT p.project_id,m.user_id FROM task_project p JOIN sys_space_member m ON m.space_id=p.space_id WHERE p.deleted=0;

CREATE TABLE project_report (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,project_id VARCHAR(32) NOT NULL,author_id BIGINT NOT NULL,
 title VARCHAR(150) NOT NULL,content TEXT NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'submitted',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_report_status CHECK(status IN ('submitted','reviewed')),
 KEY idx_report_project(project_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE project_report_reply (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,report_id BIGINT NOT NULL,author_id BIGINT NOT NULL,
 content TEXT NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(report_id) REFERENCES project_report(id),KEY idx_report_reply(report_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
