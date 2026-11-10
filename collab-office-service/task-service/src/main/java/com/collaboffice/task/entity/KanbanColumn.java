package com.collaboffice.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 看板自定义列实体
 */
@Data
@TableName("task_kanban_column")
public class KanbanColumn {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属项目ID */
    private String projectId;
    /** 列名称 */
    private String name;
    /** 排序序号 */
    private Integer sortOrder;
    /** WIP 在制品上限（0=不限） */
    private Integer wipLimit;
    /** 列颜色（十六进制色值） */
    private String color;
    /** 是否为完成列：0=否 1=是 */
    private Integer isDoneColumn;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
