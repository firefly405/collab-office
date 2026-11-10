package com.collaboffice.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务依赖关系（task_id 依赖于 depends_on_task_id 先完成）
 */
@Data
@TableName("task_dependency")
public class TaskDependency {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String dependsOnTaskId;
    private Long creatorId;
    private LocalDateTime createTime;
}
