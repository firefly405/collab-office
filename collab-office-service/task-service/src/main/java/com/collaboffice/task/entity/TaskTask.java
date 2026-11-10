package com.collaboffice.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task_task")
public class TaskTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String projectId;
    private String parentId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long columnId;
    private String repeatRule;
    private String repeatNextId;
    private Integer reminderEnabled;
    private Integer reminderDays;
    private String reminderFrequency;
    private LocalDateTime lastRemindedAt;
    private String title;
    private String description;
    private Integer status;
    private Integer priority;
    private Long assigneeId;
    private String assigneeName;
    private Long creatorId;
    private String creatorName;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime completedDate;
    private java.math.BigDecimal estimatedHours;
    private java.math.BigDecimal actualHours;
    private Integer progress;
    private String tags;
    private String attachments;
    private String checklist;
    private Integer sortOrder;
    private Integer isArchived;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
