package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_record")
public class MeetingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String meetingId;
    private String minuteContent;
    private String todoListJson;
    private String recordingUrl;
    private Integer duration;
    private LocalDateTime createTime;
}
