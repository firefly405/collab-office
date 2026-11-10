package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotNull(message = "文档类型不能为空")
    private Integer type;
    private Long spaceId;
    private Long folderId;
    private String templateId;
}
