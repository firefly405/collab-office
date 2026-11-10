package com.collaboffice.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_item")
public class KnowledgeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String knowledgeId;
    private String title;
    private String content;
    private String tagsJson;
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy=com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private Long categoryId;
    private Long spaceId;
    private Integer status;
    private Long reviewerId;
    private Long contributorId;
    private String contributorName;
    private Integer viewCount;
    private Integer starCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
