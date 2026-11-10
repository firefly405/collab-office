package com.collaboffice.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    private static volatile Snowflake snowflake;

    @Value("${snowflake.worker-id:1}")
    private long workerId;

    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;

    @PostConstruct
    public void init() {
        snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }

    public static long nextId() {
        if (snowflake == null) {
            snowflake = IdUtil.getSnowflake(1, 1);
        }
        return snowflake.nextId();
    }

    public static String nextIdStr() {
        if (snowflake == null) {
            snowflake = IdUtil.getSnowflake(1, 1);
        }
        return snowflake.nextIdStr();
    }
}
