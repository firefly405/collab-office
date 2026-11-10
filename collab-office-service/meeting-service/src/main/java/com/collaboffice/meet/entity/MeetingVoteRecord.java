package com.collaboffice.meet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_vote_record")
public class MeetingVoteRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String voteId;
    private String meetingId;
    private Long userId;
    private String selectedOptions;  // JSON array of selected option indices
    private LocalDateTime createTime;
}
