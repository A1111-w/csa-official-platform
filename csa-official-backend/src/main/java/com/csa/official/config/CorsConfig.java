package com.csa.official.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public CorsConfig(@Value("${csa.security.cors.allowed-origin-patterns:}") String allowedOriginPatterns) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        boolean hasWildcard = Arrays.stream(this.allowedOriginPatterns)
                .anyMatch(pattern -> pattern.contains("*"));

        if (hasWildcard) {
            throw new IllegalStateException("Wildcard CORS origin patterns are not allowed when credentials are enabled.");
        }

        // fail-closed：空配置会导致 Spring 默认放行所有来源，必须在启动时拦截
        if (this.allowedOriginPatterns.length == 0) {
            throw new IllegalStateException(
                    "csa.security.cors.allowed-origin-patterns 不能为空。" +
                    "请在环境变量或配置文件中设置 CORS_ALLOWED_ORIGIN_PATTERNS，" +
                    "或使用默认值（localhost:3000 等）。");
        }
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition", "X-Request-ID")
                .allowCredentials(true);
    }
}
