package com.collaboffice.common.util;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;

public class WebUtil {

    public static String getClientIp(HttpServletRequest request) {
        return JakartaServletUtil.getClientIP(request);
    }

    public static String getRequestUrl(HttpServletRequest request) {
        return request.getRequestURI();
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
