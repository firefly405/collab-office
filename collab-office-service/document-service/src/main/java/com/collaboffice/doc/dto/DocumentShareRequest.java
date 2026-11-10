package com.collaboffice.doc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentShareRequest {
    private String docId;
    @NotNull
    private Integer permission;
    @jakarta.validation.constraints.Size(max=20)
    private String password;
    @Min(1)
    private Integer expireDays;
}
