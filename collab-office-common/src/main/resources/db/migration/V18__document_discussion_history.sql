ALTER TABLE doc_document ADD COLUMN version_retention INT NOT NULL DEFAULT 100;
ALTER TABLE doc_document ADD CONSTRAINT chk_version_retention CHECK (version_retention BETWEEN 10 AND 1000);
ALTER TABLE doc_member DROP CHECK chk_doc_permission;
ALTER TABLE doc_member ADD CONSTRAINT chk_doc_permission CHECK (permission IN (1,2,3,4));
ALTER TABLE doc_member MODIFY permission TINYINT DEFAULT 1 COMMENT '1 view, 2 edit, 3 manage, 4 comment only';
