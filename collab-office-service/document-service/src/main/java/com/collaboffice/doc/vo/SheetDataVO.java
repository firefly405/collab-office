package com.collaboffice.doc.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SheetDataVO {
    private String sheetId;
    private String title;
    private List<SheetSheetVO> sheets;
}


