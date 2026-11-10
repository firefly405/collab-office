package com.collaboffice.doc.vo;

import lombok.Data;

@Data
public class CellVO {
    private String cellKey;
    private String content;
    private String formatJson;
    private String formula;
    private String resultValue;
}
