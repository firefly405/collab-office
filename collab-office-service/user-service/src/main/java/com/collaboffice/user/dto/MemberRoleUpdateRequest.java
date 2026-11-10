package com.collaboffice.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberRoleUpdateRequest {

    @NotNull(message = "空间ID不能为空")
    private Long spaceId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "角色类型不能为空")
    @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(3)
    private Integer roleType;
}
