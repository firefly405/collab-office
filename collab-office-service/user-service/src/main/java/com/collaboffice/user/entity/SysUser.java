package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private Integer status;
    private Integer onlineStatus;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;

    /** OAuth 三方登录提供商 (wechat / wecom) */
    private String oauthProvider;
    /** OAuth 三方登录 OpenID */
    private String oauthOpenId;
    /** MFA TOTP 密钥 (Base32 编码) */
    private String mfaSecret;
    /** MFA 是否启用：0=否 1=是 */
    private Integer mfaEnabled;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
