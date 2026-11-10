package com.collaboffice.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String avatar;
    private String signature;
    private Integer gender;
}
