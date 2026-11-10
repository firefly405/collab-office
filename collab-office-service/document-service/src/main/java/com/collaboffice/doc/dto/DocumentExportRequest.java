package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentExportRequest {
    private String docId;
    @NotBlank
    private String format;
    private Boolean includeComments;
    private Boolean includeVersions;
}
