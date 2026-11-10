package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("whiteboard_element")
public class WhiteboardElement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String boardId;
    private String elementType;
    private String positionJson;
    private String sizeJson;
    private String styleJson;
    private String content;
    private Integer zIndex;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
