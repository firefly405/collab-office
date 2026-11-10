package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mindmap_snapshot")
public class MindmapSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String mindmapId;
    private String snapshotData;
    private Long createdBy;
    private String label;
    private LocalDateTime createdAt;
}
