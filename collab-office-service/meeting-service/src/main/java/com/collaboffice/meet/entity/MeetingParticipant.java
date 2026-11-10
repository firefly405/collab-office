package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_participant")
public class MeetingParticipant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String meetingId;
    private Long userId;
    private String userName;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private Integer duration;
    private Integer roleType;
}
