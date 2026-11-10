package com.collaboffice.doc.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VersionVO {
    private String docId;
    private Long version;
    private String versionName;
    private String editorName;
    private String changeSummary;
    private LocalDateTime createdTime;
    private Integer isSnapshot;
}
