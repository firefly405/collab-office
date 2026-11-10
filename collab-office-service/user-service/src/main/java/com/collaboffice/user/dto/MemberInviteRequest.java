package com.collaboffice.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MemberInviteRequest {

    @NotNull(message = "空间ID不能为空")
    private Long spaceId;

    @NotEmpty(message = "成员ID列表不能为空")
    private List<Long> memberIds;

    private Integer roleType;
}
