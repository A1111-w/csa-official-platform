package com.csa.official.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionStartupValidatorTest {

    @Test
    void nonProductionProfileDoesNotRequireDeploymentSettings() {
        assertThatCode(() -> validator(new MockEnvironment(), "", "memory", "", "", "none", false))
                .doesNotThrowAnyException();
    }

    @Test
    void productionRequiresHttps() {
        MockEnvironment environment = productionEnvironment();

        assertThatThrownBy(() -> validator(environment, "http://example.edu", "redis", "redis",
                "172.30.0.0/24", "framework", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void productionRequiresRedisAndTrustedProxy() {
        MockEnvironment environment = productionEnvironment();

        assertThatThrownBy(() -> validator(environment, "https://example.edu", "memory", "redis",
                "172.30.0.0/24", "framework", true))
                .hasMessageContaining("cache.type=redis");

        assertThatThrownBy(() -> validator(environment, "https://example.edu", "redis", "redis",
                "", "framework", true))
                .hasMessageContaining("trusted proxy");
    }

    @Test
    void completeProductionSettingsPass() {
        assertThatCode(() -> validator(productionEnvironment(), "https://csa.example.edu", "redis", "redis",
                "172.30.0.0/24", "framework", true))
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }

    private ProductionStartupValidator validator(
            MockEnvironment environment,
            String publicBaseUrl,
            String cacheType,
            String redisHost,
            String trustedProxies,
            String forwardHeadersStrategy,
            boolean csrfEnabled) {
        return new ProductionStartupValidator(environment, publicBaseUrl, cacheType, redisHost,
                trustedProxies, forwardHeadersStrategy, csrfEnabled);
    }
}
