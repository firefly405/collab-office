-- Place legacy unscoped local uploads in their creator's first team.
UPDATE file_file f SET f.space_id = (SELECT MIN(m.space_id) FROM sys_space_member m WHERE m.user_id=f.uploader_id)
WHERE f.space_id IS NULL;
UPDATE file_folder f SET f.space_id = (SELECT MIN(m.space_id) FROM sys_space_member m WHERE m.user_id=f.creator_id)
WHERE f.space_id IS NULL;

CREATE TABLE IF NOT EXISTS doc_user_favorite (
  user_id bigint NOT NULL,
  doc_id varchar(32) NOT NULL,
  PRIMARY KEY (user_id, doc_id)
);
