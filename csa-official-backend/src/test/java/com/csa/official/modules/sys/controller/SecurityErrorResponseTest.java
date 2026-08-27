package com.csa.official.modules.sys.controller;

import com.csa.official.common.cache.MemoryKeyValueStore;
import com.csa.official.common.observability.RequestIdFilter;
import com.csa.official.common.security.JwtAccessDeniedHandler;
import com.csa.official.common.security.JwtAuthenticationEntryPoint;
import com.csa.official.common.security.CsrfTokenService;
import com.csa.official.common.security.JwtRevocationService;
import com.csa.official.common.security.SecurityResponseWriter;
import com.csa.official.config.CsrfProtectionFilter;
import com.csa.official.config.CorsConfig;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.config.JwtAuthenticationFilter;
import com.csa.official.config.SecurityConfig;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@ContextConfiguration(classes = SecurityErrorResponseTest.TestConfig.class)
@WebAppConfiguration
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "csa.jwt.secret=01234567890123456789012345678901",
        "csa.jwt.expiration=604800000",
        "csa.security.cors.allowed-origin-patterns=http://localhost:3000"
})
class SecurityErrorResponseTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RequestIdFilter requestIdFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedRequestReturns401Json() throws Exception {
        mockMvc.perform(get("/api/test/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void invalidTokenReturns401Json() throws Exception {
        when(jwtUtils.isTokenExpired("bad-token")).thenThrow(new JwtException("invalid token"));

        mockMvc.perform(get("/api/test/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void authenticationDependencyFailureReturns500Json() throws Exception {
        when(jwtUtils.isTokenExpired("dependency-failure-token"))
                .thenThrow(new IllegalStateException("database unavailable"));

        mockMvc.perform(get("/api/test/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dependency-failure-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void authenticatedWithoutAdminRoleReturns403Json() throws Exception {
        mockMvc.perform(get("/api/test/users")
                        .with(SecurityMockMvcRequestPostProcessors.user("member").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void trustedOriginPreflightReceivesCredentialedCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/test/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void untrustedOriginPreflightIsRejected() throws Exception {
        mockMvc.perform(options("/api/test/users")
                        .header(HttpHeaders.ORIGIN, "https://attacker.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void secureResponseIncludesBrowserSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/test/users").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Strict-Transport-Security",
                        containsString("max-age=63072000")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("default-src 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy",
                        containsString("camera=()")));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            CorsConfig.class,
            TestController.class,
            CsrfProtectionFilter.class,
            CsrfTokenService.class,
            JwtRevocationService.class,
            MemoryKeyValueStore.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            SecurityResponseWriter.class,
            RequestIdFilter.class
    })
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        UserMapper userMapper() {
            return Mockito.mock(UserMapper.class);
        }

        @Bean
        JwtUtils jwtUtils() {
            return Mockito.mock(JwtUtils.class);
        }

        @Bean
        UserDetailsServiceImpl userDetailsService() {
            return Mockito.mock(UserDetailsServiceImpl.class);
        }
    }
}
