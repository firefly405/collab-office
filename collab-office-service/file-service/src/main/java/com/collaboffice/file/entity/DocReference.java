package com.collaboffice.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_reference")
public class DocReference {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceDocId;
    private String targetDocId;
    private Long creatorId;
    private LocalDateTime createTime;
}
