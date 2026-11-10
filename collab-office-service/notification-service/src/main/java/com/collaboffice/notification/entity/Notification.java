package com.collaboffice.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private String relatedId;
    @TableField(exist = false)
    private String relatedType;
    private Integer isRead;
    private LocalDateTime readTime;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
    private Long seqId;
}
