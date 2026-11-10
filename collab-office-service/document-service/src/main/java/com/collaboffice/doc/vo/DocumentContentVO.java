package com.collaboffice.doc.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentContentVO {
    private String docId;
    private String title;
    private String content;
    private Long version;
    private String lastEditor;
    private LocalDateTime lastEditTime;
}
