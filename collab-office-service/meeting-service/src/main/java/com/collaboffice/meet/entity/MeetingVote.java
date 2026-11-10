package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_vote")
public class MeetingVote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String voteId;
    private String meetingId;
    private String title;
    private String description;
    private String options;  // JSON array of option texts
    private Long creatorId;
    private Integer status;  // 0=进行中 1=已结束
    private LocalDateTime createTime;
    private LocalDateTime endTime;
}
