package com.collaboffice.common.config;

import com.collaboffice.common.interceptor.RequestSignatureInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcConfig implements WebMvcConfigurer {

    private final org.springframework.beans.factory.ObjectProvider<com.collaboffice.common.security.WorkspaceAccess> access;

    public WebMvcConfig(org.springframework.beans.factory.ObjectProvider<com.collaboffice.common.security.WorkspaceAccess> access) { this.access = access; }

    @Value("${app.internal-token:default-secret-token}")
    private String secretToken;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册内部调用签名拦截器，排除开发文档与公共路径
        registry.addInterceptor(new RequestSignatureInterceptor(secretToken))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/v1/health",
                        "/api/v1/auth/**",
                        "/api/v1/document/share/**",
                        "/public/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/error",
                        "/actuator/**"
                );
        access.ifAvailable(workspace -> registry.addInterceptor(new com.collaboffice.common.security.ResourceAccessInterceptor(workspace))
                .addPathPatterns("/api/v1/admin/**", "/api/v1/space/**", "/api/v1/org/**", "/api/v1/document/**", "/api/v1/file/**", "/api/v1/task/**", "/api/v1/meeting/**", "/api/v1/chat/**", "/api/v1/collaboration/**", "/api/v1/sheet/**", "/api/v1/mindmap/**", "/api/v1/whiteboard/**"));
    }
}
