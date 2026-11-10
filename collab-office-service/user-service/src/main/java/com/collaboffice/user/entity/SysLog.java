package com.collaboffice.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userName;
    private String operation;
    private String module;
    private String requestIp;
    private String requestUrl;
    private String method;
    private String paramsJson;
    private String resultJson;
    private Long costTime;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}
