package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_space_member")
public class SysSpaceMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spaceId;
    private Long userId;
    private Integer roleType;
    private LocalDateTime joinTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
