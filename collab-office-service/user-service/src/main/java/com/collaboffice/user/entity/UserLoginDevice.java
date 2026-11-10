package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_login_device")
public class UserLoginDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private LocalDateTime lastLoginTime;
    private String tokenId;
    private Integer isCurrent;
    private LocalDateTime createTime;
}
