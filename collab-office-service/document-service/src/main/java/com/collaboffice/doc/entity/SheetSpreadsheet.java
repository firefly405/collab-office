package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sheet_spreadsheet")
public class SheetSpreadsheet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sheetId;
    private String title;
    private Long spaceId;
    private Long folderId;
    private Long creatorId;
    @TableField(exist = false)
    private String creatorName;
    private Integer sheetCount;
    private Integer rowCount;
    private Integer colCount;
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
