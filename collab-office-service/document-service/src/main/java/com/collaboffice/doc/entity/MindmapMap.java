package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mindmap_map")
public class MindmapMap {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String mapId;
    private String title;
    private String layoutType;
    private String themeJson;
    private Long spaceId;
    private Long creatorId;
    private LocalDateTime lastEditTime;
    @TableField(exist = false)
    private Long lastEditorId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
