package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sheet_sheet")
public class SheetSheet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String spreadsheetId;
    private String sheetName;
    private Integer sheetIndex;
    private Integer rowCount;
    private Integer colCount;
    private Integer sortOrder;
    private String settingsJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
