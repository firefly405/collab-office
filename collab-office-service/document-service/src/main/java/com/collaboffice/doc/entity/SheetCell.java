package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sheet_cell")
public class SheetCell {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sheetId;
    private String cellKey;
    private Integer rowIndex;
    private Integer colIndex;
    private String content;
    private String formatJson;
    private String formula;
    private String resultValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
