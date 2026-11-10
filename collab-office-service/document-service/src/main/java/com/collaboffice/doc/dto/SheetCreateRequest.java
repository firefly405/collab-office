package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SheetCreateRequest {
    @NotBlank
    private String title;
    private Long spaceId;
    private Long folderId;
}
