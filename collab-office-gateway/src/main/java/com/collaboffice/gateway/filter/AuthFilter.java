package com.collaboffice.gateway.filter;

import com.collaboffice.common.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtTokenUtil jwtTokenUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/health",
            "/api/v1/health",
            "/api/v1/auth/**",
            "/api/v1/document/share/**",
            "/public/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    @org.springframework.beans.factory.annotation.Value("${app.internal-token:default-secret-token}")
    private String internalToken;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.contains("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        // 跳过无需鉴权的路径
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // 检查OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("未携带Token: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtTokenUtil.validateToken(token)) {
            log.warn("Token无效或已过期: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String tokenId = jwtTokenUtil.getJtiFromToken(token);
        if (tokenId != null) {
            return redisTemplate.hasKey("token:blacklist:" + tokenId)
                    .flatMap(hasKey -> {
                        if (Boolean.TRUE.equals(hasKey)) {
                            log.warn("Token已加入黑名单: {}", tokenId);
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                        return proceedFilter(exchange, chain, token, path);
                    });
        }

        return proceedFilter(exchange, chain, token, path);
    }

    private Mono<Void> proceedFilter(ServerWebExchange exchange, GatewayFilterChain chain, String token, String path) {
        String userId = jwtTokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = calculateSignature(userId, timestamp, path, internalToken);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> { h.remove("X-Internal-Token"); h.remove("X-User-Id"); h.remove("X-User-Name"); h.remove("X-Request-Signature"); h.remove("X-Request-Timestamp"); })
                .header("X-User-Id", userId)
                .header("X-Request-Timestamp", timestamp)
                .header("X-Request-Signature", signature)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private String calculateSignature(String userId, String timestamp, String path, String secret) {
        try {
            String data = userId + ":" + timestamp + ":" + path;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to calculate gateway request signature: {}", e.getMessage());
            return "";
        }
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
