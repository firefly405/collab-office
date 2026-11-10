package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_document")
public class DocDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docId;
    private String title;
    private Integer type;
    private Long spaceId;
    private Long folderId;
    private Long creatorId;
    private String creatorName;
    private Long currentVersion;
    private Integer wordCount;
    private Integer isPublic;
    private Integer isTemplate;
    private Integer isFavorite;
    private Integer isLocked;
    private Integer status;
    private LocalDateTime lastEditTime;
    private Long lastEditorId;
    private String lastEditorName;
    private Integer editCount;
    private Integer viewCount;
    private String tags;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
