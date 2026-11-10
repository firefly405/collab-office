package com.collaboffice.msg.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_session_member")
public class ChatSessionMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Long userId;
    private String role;
    private String nickname;
    private Integer isMuted;
    private Integer unreadCount;
    private LocalDateTime joinedTime;
    @TableField(exist = false)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
