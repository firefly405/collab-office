package com.collaboffice.doc.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentDetailVO {
    private Boolean canEdit;
    private Boolean canComment;
    private String docId;
    private String title;
    private Integer type;
    private String creatorName;
    private String lastEditorName;
    private Long currentVersion;
    private Integer wordCount;
    private Integer editCount;
    private Integer viewCount;
    private String tags;
    private Integer isFavorite;
    private Integer isPinned;
    private Long folderId;
    private LocalDateTime createTime;
    private LocalDateTime lastEditTime;
    private Integer memberCount;
    private Integer onlineMemberCount;
}
