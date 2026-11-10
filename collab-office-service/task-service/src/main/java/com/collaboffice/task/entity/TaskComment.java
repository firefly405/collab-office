package com.collaboffice.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_comment")
public class TaskComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private Long userId;
    private String userName;
    private String content;
    private String mentionUsersJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
