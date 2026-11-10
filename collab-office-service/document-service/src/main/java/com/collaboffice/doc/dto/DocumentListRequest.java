package com.collaboffice.doc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentListRequest {
    @NotNull
    private Long spaceId;
    private Long userId;
    private String filter;
    @Min(0) @Max(1)
    private Integer status = 1;
    private Integer type;
    private String keyword;
    private String tag;
    private Long folderId;
    @Min(1)
    private Integer pageNum = 1;
    @Min(1) @Max(100)
    private Integer pageSize = 20;
}
