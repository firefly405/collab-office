package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CellBatchUpdateRequest {
    @NotNull
    private Long sheetId;
    @NotEmpty
    private List<CellUpdateRequest> cells;
}
