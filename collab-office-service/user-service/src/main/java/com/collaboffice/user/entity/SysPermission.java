package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String permissionName;
    private String permissionCode;
    private Integer type;
    private String icon;
    private Integer sort;
    @TableField(exist = false)
    private String path;
    @TableField(exist = false)
    private String component;
    private Integer status;
    private LocalDateTime createTime;
    @TableField(exist = false)
    private LocalDateTime updateTime;
}
