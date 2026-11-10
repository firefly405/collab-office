package com.collaboffice.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {

    private Long userId;
    private String username;
    private String platformRole;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private Integer status;
    private Integer onlineStatus;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
}
