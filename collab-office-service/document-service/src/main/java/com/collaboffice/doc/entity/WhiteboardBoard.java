package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("whiteboard_board")
public class WhiteboardBoard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String boardId;
    private String title;
    private Integer width;
    private Integer height;
    private String backgroundColor;
    private Long spaceId;
    private Long creatorId;
    private LocalDateTime lastEditTime;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
