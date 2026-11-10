package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_space")
public class SysSpace {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String spaceName;
    private String spaceLogo;
    private Long ownerId;
    private Long storageQuota;
    private Long storageUsed;
    @TableField("security_config_json")
    private String securityConfig;
    private Integer status;
    @TableField("description")
    private String remark;

    @TableField(exist = false)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
