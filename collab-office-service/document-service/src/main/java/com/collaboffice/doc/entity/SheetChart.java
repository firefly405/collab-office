package com.collaboffice.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sheet_chart")
public class SheetChart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sheetId;
    private String chartType;
    private String configJson;
    private String positionJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
