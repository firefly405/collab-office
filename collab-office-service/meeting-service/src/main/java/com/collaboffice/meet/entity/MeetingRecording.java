package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_recording")
public class MeetingRecording {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordingId;
    private String meetingId;
    private String title;
    private String minioObjectKey;
    private Long fileSize;
    private Integer duration;  // seconds
    private Integer status;    // 0=录制中 1=完成 2=失败
    private Long uploaderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
