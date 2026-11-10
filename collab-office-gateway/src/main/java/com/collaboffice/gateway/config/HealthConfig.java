package com.collaboffice.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import java.util.Map;

@Configuration
public class HealthConfig {

    @Bean
    public RouterFunction<ServerResponse> healthRoute() {
        return RouterFunctions.route()
            .GET("/api/health", request ->
                ServerResponse.ok().bodyValue(Map.of(
                    "status", "UP",
                    "service", "collab-office-gateway"
                ))
            )
            .GET("/api/v1/health", request ->
                ServerResponse.ok().bodyValue(Map.of(
                    "status", "UP",
                    "service", "collab-office-gateway"
                ))
            )
            .build();
    }
}
