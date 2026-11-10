package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CellUpdateRequest {
    @NotNull
    private Long sheetId;
    @NotBlank
    private String cellKey;
    private String content;
    private String formatJson;
    private String formula;
    private Boolean checkConflict;
    private String expectedContent;
    private String expectedFormatJson;
}
