package com.collaboffice.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
public class RequestSignatureInterceptor implements HandlerInterceptor {

    private final String secretToken;

    public RequestSignatureInterceptor(String secretToken) {
        this.secretToken = secretToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 1. 优先校验内部直接调用Token (Feign调用等)
        String internalToken = request.getHeader("X-Internal-Token");
        if (internalToken != null) {
            if (secretToken.equals(internalToken)) {
                return true;
            } else {
                log.warn("Access Denied: Invalid X-Internal-Token on path: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"未授权的内部请求凭证\"}");
                return false;
            }
        }

        // 2. 校验来自网关透传的签名
        String requestSignature = request.getHeader("X-Request-Signature");
        String requestTimestamp = request.getHeader("X-Request-Timestamp");
        String userId = request.getHeader("X-User-Id");

        if (requestSignature == null || requestTimestamp == null || userId == null) {
            log.warn("Access Denied: Missing signature headers on path: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权的内部访问，缺少安全凭证\"}");
            return false;
        }

        // 验证时间戳防重放攻击 (5分钟内有效)
        try {
            long timestamp = Long.parseLong(requestTimestamp);
            long now = System.currentTimeMillis();
            if (Math.abs(now - timestamp) > 1000L * 60 * 5) {
                log.warn("Access Denied: Signature expired on path: {}, time diff: {}ms", path, (now - timestamp));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"请求凭证已过期\"}");
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Access Denied: Invalid timestamp: {}", requestTimestamp);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 验证 HmacSHA256 签名
        String expectedSignature = calculateSignature(userId, requestTimestamp, path, secretToken);
        if (!expectedSignature.equals(requestSignature)) {
            log.warn("Access Denied: Signature mismatch on path: {}. Expected: {}, Actual: {}", path, expectedSignature, requestSignature);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请求签名验证失败\"}");
            return false;
        }

        return true;
    }

    private String calculateSignature(String userId, String timestamp, String path, String secret) {
        try {
            String data = userId + ":" + timestamp + ":" + path;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to calculate request signature: {}", e.getMessage());
            return "";
        }
    }
}
