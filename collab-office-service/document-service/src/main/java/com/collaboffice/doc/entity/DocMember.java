package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_member")
public class DocMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docId;
    private Long userId;
    private String userName;
    private Integer permission;
    private Integer isOnline;
    private LocalDateTime lastActiveTime;
    private LocalDateTime joinTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
