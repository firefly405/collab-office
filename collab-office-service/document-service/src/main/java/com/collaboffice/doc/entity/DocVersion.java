package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_version")
public class DocVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docId;
    private Long version;
    private String versionName;
    private String snapshotData;
    private Long editorId;
    private String editorName;
    private String changeSummary;
    private String contentHash;
    private Integer isSnapshot;
    private LocalDateTime createTime;
}
