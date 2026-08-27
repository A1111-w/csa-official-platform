package com.csa.official.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SecurityStartupValidator {

    public SecurityStartupValidator(Environment environment,
                                    @Value("${csa.security.cookie.secure:false}") boolean authCookieSecure,
                                    @Value("${csa.security.cookie.same-site:Lax}") String authCookieSameSite) {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));

        if ("None".equalsIgnoreCase(authCookieSameSite) && !authCookieSecure) {
            throw new IllegalStateException("SameSite=None auth cookies must also set Secure=true.");
        }

        if (productionProfile && !authCookieSecure) {
            throw new IllegalStateException("Auth cookies must set Secure=true under the prod/production profile.");
        }
    }
}
