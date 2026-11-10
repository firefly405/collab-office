package com.collaboffice.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpaceUpdateRequest {

    @NotNull(message = "空间ID不能为空")
    private Long spaceId;

    private String spaceName;
    private String spaceLogo;
    private String description;
    private Long storageQuota;
}
