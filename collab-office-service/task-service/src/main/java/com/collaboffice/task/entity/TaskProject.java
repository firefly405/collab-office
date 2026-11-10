package com.collaboffice.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task_project")
public class TaskProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectId;
    private String name;
    private String description;
    private Long spaceId;
    private Integer status;
    private Integer progress;
    private String milestonesJson;
    private LocalDate startDate;
    private LocalDate endDate;
    @TableField("creator_id")
    private Long createBy;
    private LocalDateTime createTime;
    @TableField(exist = false)
    private Long updateBy;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
