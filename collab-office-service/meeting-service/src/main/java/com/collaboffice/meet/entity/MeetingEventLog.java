package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_event_log")
public class MeetingEventLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String meetingId;
    private Long operatorId;
    private String eventType;
    private Long targetUserId;
    private LocalDateTime eventTime;
}
