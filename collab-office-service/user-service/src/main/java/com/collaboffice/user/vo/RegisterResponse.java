package com.collaboffice.user.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {

    private Long userId;
    private String accessToken;
    private String refreshToken;
    private Long expireIn;
}
