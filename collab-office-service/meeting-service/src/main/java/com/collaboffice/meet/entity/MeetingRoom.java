package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_room")
public class MeetingRoom {
    public static final int SCHEDULED = 1;
    public static final int IN_PROGRESS = 2;
    public static final int ENDED = 3;
    public static final int CANCELLED = 4;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomId;
    private String title;
    private String agenda;
    private Long hostId;
    private String hostName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Integer status;
    private String recordUrl;
    private String password;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
