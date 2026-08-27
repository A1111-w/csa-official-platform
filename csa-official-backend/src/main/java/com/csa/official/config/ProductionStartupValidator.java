package com.csa.official.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Arrays;

@Component
public class ProductionStartupValidator {

    public ProductionStartupValidator(
            Environment environment,
            @Value("${csa.public-base-url:}") String publicBaseUrl,
            @Value("${csa.cache.type:redis}") String cacheType,
            @Value("${spring.data.redis.host:}") String redisHost,
            @Value("${csa.security.rate-limit.trusted-proxies:}") String trustedProxies,
            @Value("${server.forward-headers-strategy:none}") String forwardHeadersStrategy,
            @Value("${csa.security.csrf.enabled:true}") boolean csrfEnabled) {
        if (!isProduction(environment)) {
            return;
        }

        requireHttpsBaseUrl(publicBaseUrl);
        require("redis".equalsIgnoreCase(cacheType),
                "Production requires csa.cache.type=redis.");
        require(StringUtils.hasText(redisHost),
                "Production requires a Redis host.");
        require(StringUtils.hasText(trustedProxies),
                "Production requires an explicit trusted proxy or CIDR.");
        require("framework".equalsIgnoreCase(forwardHeadersStrategy),
                "Production requires server.forward-headers-strategy=framework behind the same-origin proxy.");
        require(csrfEnabled, "Production requires CSRF protection.");
    }

    private boolean isProduction(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod")
                        || profile.equalsIgnoreCase("production"));
    }

    private void requireHttpsBaseUrl(String publicBaseUrl) {
        require(StringUtils.hasText(publicBaseUrl),
                "Production requires csa.public-base-url/PUBLIC_BASE_URL.");
        try {
            URI uri = URI.create(publicBaseUrl);
            require("https".equalsIgnoreCase(uri.getScheme()) && StringUtils.hasText(uri.getHost())
                            && uri.getUserInfo() == null,
                    "Production public base URL must be an HTTPS origin without credentials.");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Production public base URL is invalid.", e);
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
