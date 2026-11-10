package com.collaboffice.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SpaceMemberVO {

    private Long userId;
    private String userName;
    private String avatar;
    private Integer roleType;
    private LocalDateTime joinTime;
    private Integer onlineStatus;
    private LocalDateTime lastActiveTime;
}
