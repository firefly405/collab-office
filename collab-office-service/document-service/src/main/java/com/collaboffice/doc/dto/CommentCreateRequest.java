package com.collaboffice.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CommentCreateRequest {
    @NotNull
    private String docId;
    @NotBlank
    private String content;
    private String selectionRange;
    private Long parentId;
    private List<Long> mentionUsers;
}
