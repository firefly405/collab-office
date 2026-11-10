package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class FormulaCalculateRequest {
    @NotBlank
    private String formula;
    private Map<String, String> contextCells;
}
