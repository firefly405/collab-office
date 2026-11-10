package com.collaboffice.doc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTaskVO {
    private String taskId;
    private String status;
    private Integer progress;
    private String fileUrl;
    private String errorMsg;
}
