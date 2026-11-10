package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_content")
public class DocContent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docId;
    private Long version;
    private String content;
    private String contentHash;
    private String delta;
    private Integer isSnapshot;
    private Long editorId;
    private String editorName;
    private LocalDateTime editTime;
    private String remark;
    private LocalDateTime createTime;
}
