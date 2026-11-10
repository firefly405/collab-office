package com.collaboffice.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SpaceCreateRequest {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 100, message = "空间名称最长100字")
    private String spaceName;

    private String spaceLogo;
    private String description;
}
