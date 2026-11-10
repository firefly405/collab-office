package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mindmap_node")
public class MindmapNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String mapId;
    private Long parentId;
    private String nodeKey;
    private String content;
    private String styleJson;
    private String icon;
    private String note;
    private String link;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
