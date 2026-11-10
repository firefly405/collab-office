package com.collaboffice.msg.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String sessionId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Integer msgType;
    private String content;
    private String extra;
    private String replyToId;
    private Integer isEdited;
    private Integer isRecalled;
    private LocalDateTime recallTime;
    private String mentionUsers;
    private Integer status;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
    
    private Long seqId;
    private String sourceType;
    private String sourceId;
}
