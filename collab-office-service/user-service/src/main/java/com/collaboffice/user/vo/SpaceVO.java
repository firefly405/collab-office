package com.collaboffice.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SpaceVO {

    private Long spaceId;
    private String spaceName;
    private String spaceLogo;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private Long storageQuota;
    private Long storageUsed;
    private Integer status;
    private LocalDateTime createTime;
}
