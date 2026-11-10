package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("whiteboard_template")
public class WhiteboardTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String thumbnailUrl;
    private String templateData;
    private String category;
    private Integer isSystem;
    private LocalDateTime createTime;
}
