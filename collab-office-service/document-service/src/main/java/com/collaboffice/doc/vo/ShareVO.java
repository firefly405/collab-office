package com.collaboffice.doc.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShareVO {
    private Long shareId;
    private String shareToken;
    private String shareUrl;
    private Integer permission;
    private LocalDateTime expireTime;
    private Boolean hasPassword;
}
