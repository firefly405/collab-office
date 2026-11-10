package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_share")
public class DocShare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docId;
    private String shareToken;
    private String sharePassword;
    private Integer permission;
    private LocalDateTime expireTime;
    private Integer viewCount;
    private Long creatorId;
    private Integer status;
    private LocalDateTime createTime;
}
