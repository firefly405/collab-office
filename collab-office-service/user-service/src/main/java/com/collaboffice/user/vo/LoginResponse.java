package com.collaboffice.user.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long expireIn;
    private String tokenType;
    private UserInfoVO userInfo;
    private Boolean requireMfa;
    private Long mfaUserId;
}
