package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_template")
public class TemplateTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateId;
    private String name;
    private Integer type;
    private String category;
    private String contentJson;
    private String thumbnailUrl;
    private Integer usageCount;
    private Integer isOfficial;
    private Long creatorId;
    private Long spaceId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
