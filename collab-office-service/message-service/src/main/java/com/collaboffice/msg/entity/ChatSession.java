package com.collaboffice.msg.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer sessionType;
    private String sessionName;
    private String sessionAvatar;
    private Long creatorId;
    private String lastMsgContent;
    private LocalDateTime lastMsgTime;
    private Long lastMsgSenderId;
    private Integer status;
    @TableField(exist = false)
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
