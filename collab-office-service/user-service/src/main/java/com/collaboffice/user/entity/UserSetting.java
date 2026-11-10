package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_setting")
public class UserSetting {
    @TableId
    private Long userId;
    private String language;
    private String timezone;
    private Integer emailNotify;
    private Integer pushNotify;
    private Integer privacyLevel;
    private LocalDateTime updateTime;
}
