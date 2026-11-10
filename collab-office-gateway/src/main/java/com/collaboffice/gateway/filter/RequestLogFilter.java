package com.collaboffice.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String userId = request.getHeaders().getFirst("X-User-Id");
        String clientIp = request.getHeaders().getFirst("X-Real-IP");
        if (clientIp == null) {
            clientIp = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }

        final String ip = clientIp;
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long costTime = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
            log.info("{} {} {} {} {} {}", ip, request.getMethod(), request.getURI().getPath(),
                    userId != null ? userId : "-", statusCode, costTime + "ms");
        }));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
