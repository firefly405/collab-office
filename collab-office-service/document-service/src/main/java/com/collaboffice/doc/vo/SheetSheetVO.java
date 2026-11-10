package com.collaboffice.doc.vo;

import lombok.Data;
import java.util.Map;

@Data
public class SheetSheetVO {
    private Long id;
    private String sheetName;
    private Integer sheetIndex;
    private Map<String, CellVO> cells;
    private Integer rowCount;
    private Integer colCount;
}
