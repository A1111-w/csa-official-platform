package com.csa.official.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityStartupValidatorTest {

    @Test
    void productionProfileRejectsInsecureAuthCookie() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new SecurityStartupValidator(environment, false, "Lax"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Secure=true");
    }

    @Test
    void sameSiteNoneRejectsInsecureAuthCookieInAnyProfile() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> new SecurityStartupValidator(environment, false, "None"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SameSite=None");
    }

    @Test
    void secureProductionCookiePassesStartupValidation() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThatCode(() -> new SecurityStartupValidator(environment, true, "Lax"))
                .doesNotThrowAnyException();
    }
}
