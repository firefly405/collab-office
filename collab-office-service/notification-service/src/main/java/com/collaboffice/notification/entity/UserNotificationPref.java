package com.collaboffice.notification.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_notification_pref")
public class UserNotificationPref {
    @TableId
    private Long userId;
    private Integer emailNotify;
    private Integer pushNotify;
}
