package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_comment")
public class DocComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docId;
    private Long userId;
    private String userName;
    private Long parentId;
    @TableField("selection_range_json")
    private String selectionRange;
    private String content;
    private Integer isResolved;
    private LocalDateTime resolvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
