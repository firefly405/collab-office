package com.collaboffice.doc.dto;

import lombok.Data;

@Data
public class DocumentUpdateRequest {
    private String docId;
    private String title;
    private Integer type;
    @jakarta.validation.constraints.Size(max=500)
    private String tags;
    private Long folderId;
}
