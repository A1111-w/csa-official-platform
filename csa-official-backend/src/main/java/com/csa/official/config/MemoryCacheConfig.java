package com.csa.official.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@ConditionalOnProperty(prefix = "csa.cache", name = "type", havingValue = "memory", matchIfMissing = true)
public class MemoryCacheConfig {

    public MemoryCacheConfig(Environment environment) {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));

        if (productionProfile) {
            throw new IllegalStateException("CSA_CACHE_TYPE=memory is not allowed under the prod/production profile. Use Redis for distributed rate limits and verification codes.");
        }
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "dept_list",
                "auth_user",
                "public_about",
                "public_contributors",
                "public_carousel",
                "public_contribution_rank");
    }
}
