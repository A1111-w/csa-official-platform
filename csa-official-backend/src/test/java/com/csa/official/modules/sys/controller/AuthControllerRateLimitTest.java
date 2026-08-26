package com.csa.official.modules.sys.controller;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.InviteCodeMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.MailService;
import com.csa.official.modules.sys.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "csa.cache.type=memory",
        "csa.jwt.secret=01234567890123456789012345678901",
        "csa.jwt.expiration=604800000",
        "csa.security.rate-limit.trusted-proxies=127.0.0.1"
})
@AutoConfigureMockMvc
class AuthControllerRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeyValueStore keyValueStore;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private InviteCodeMapper inviteCodeMapper;

    @MockBean
    private MailService mailService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("tester", "pwd", Collections.emptyList()));
        when(jwtUtils.generateToken(any(User.class), anyLong())).thenReturn("token");
        when(jwtUtils.getExpirationMillis()).thenReturn(604800000L);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(userMapper.selectOne(any())).thenAnswer(invocation -> buildUser(1L, "tester", RoleConsts.MEMBER));
        when(userMapper.exists(any())).thenReturn(false);
        when(userMapper.insert(any())).thenReturn(1);
        doNothing().when(mailService).verifyCode(any(), any());
        doNothing().when(mailService).sendCode(any());
    }

    @Test
    void loginSetsHttpOnlyAuthCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(forwardedIp("10.0.0.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("cookie-user", "abc123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.csrfToken").isString())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("CSA_AUTH_TOKEN=token", "HttpOnly", "SameSite=Lax"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("CSA_CSRF_TOKEN=", "SameSite=Lax")
                        .doesNotContain("HttpOnly"));
    }

    @Test
    void invalidCredentialsReturn401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(forwardedIp("10.0.0.21"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bad-user", "abc123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void databaseFailureAfterAuthenticationRemains5xx() throws Exception {
        when(userMapper.selectOne(any()))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        mockMvc.perform(post("/api/auth/login")
                        .with(forwardedIp("10.0.0.22"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("db-user", "abc123"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.errorCode").value("DATABASE_ERROR"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void logoutClearsHttpOnlyAuthCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("CSA_AUTH_TOKEN=", "Max-Age=0", "HttpOnly"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("CSA_CSRF_TOKEN=", "Max-Age=0")
                        .doesNotContain("HttpOnly"));
    }

    @Test
    void logoutRevokesBearerTokenUntilItExpires() throws Exception {
        when(jwtUtils.getTokenIdFromToken("token")).thenReturn("logout-jti");
        when(jwtUtils.getRemainingValidityMillis("token")).thenReturn(1000L);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk());

        assertThat(keyValueStore.hasKey("auth:jwt:revoked:jti:logout-jti")).isTrue();
    }

    @Test
    void csrfEndpointIssuesReadableCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.csrfToken").isString())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("CSA_CSRF_TOKEN=", "SameSite=Lax")
                        .doesNotContain("HttpOnly"));
    }

    @Test
    void merchantNoRegistrationDoesNotPromoteMembership() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "merchantUser",
                                "abc123",
                                "merchant@example.com",
                                "Tester",
                                "20230001",
                                "CSA",
                                "Class 1",
                                 null,
                                 "ORDER-123",
                                 "654321",
                                 "2026-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoleLevel()).isEqualTo(RoleConsts.GUEST);
    }

    @Test
    void loginRateLimitIsScopedByUsernameAndForwardedIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(forwardedIp("10.0.0.11"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("alice", "abc123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(forwardedIp("10.0.0.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "abc123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        mockMvc.perform(post("/api/auth/login")
                        .with(forwardedIp("10.0.0.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bob", "abc123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(forwardedIp("10.0.0.12"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("alice", "abc123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    void registerRateLimitIsScopedByUsernameAndEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "register-user",
                "abc123",
                "register@example.com",
                "Tester",
                "20230002",
                "CSA",
                "Class 2",
                 null,
                 null,
                 "123456",
                 "2026-01");

        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.13"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.13"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.13"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        RegisterRequest differentEmail = new RegisterRequest(
                "register-user",
                "abc123",
                "other@example.com",
                "Tester",
                "20230003",
                "CSA",
                "Class 3",
                 null,
                 null,
                 "123456",
                 "2026-01");

        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.13"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(differentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void sendCodeRateLimitIsScopedByEmail() throws Exception {
        mockMvc.perform(post("/api/auth/send-code")
                        .with(forwardedIp("10.0.0.14"))
                        .param("email", "same@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/send-code")
                        .with(forwardedIp("10.0.0.14"))
                        .param("email", "same@example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        mockMvc.perform(post("/api/auth/send-code")
                        .with(forwardedIp("10.0.0.14"))
                        .param("email", "other@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void registrationRejectsStalePrivacyConsentVersion() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "privacy-user", "abc123", "privacy@example.com", "Tester", "20230004",
                "CSA", "Class 4", null, null, "654321", "old-policy");

        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.15"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userMapper, org.mockito.Mockito.never()).insert(any(User.class));
    }

    @Test
    void registrationNormalizesEmailAndRejectsStudentIdCollision() throws Exception {
        when(userMapper.exists(any())).thenReturn(false, false, true);
        RegisterRequest request = new RegisterRequest(
                "student-user", "abc123", "STUDENT@EXAMPLE.COM", "Tester", "st-2026",
                "CSA", "Class 5", null, null, "654321", "2026-01");

        mockMvc.perform(post("/api/auth/register")
                        .with(forwardedIp("10.0.0.16"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        verify(userMapper, org.mockito.Mockito.never()).insert(any(User.class));
    }

    private RequestPostProcessor forwardedIp(String ip) {
        return request -> {
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", ip);
            return request;
        };
    }

    private User buildUser(Long id, String username, int roleLevel) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoleLevel(roleLevel);
        user.setDeleted(0);
        return user;
    }

    private record LoginRequest(String username, String password) {
    }

    @SuppressWarnings("unused")
    private record RegisterRequest(
            String username,
            String password,
            String email,
            String realName,
            String studentId,
            String college,
            String className,
             String inviteCode,
             String merchantNo,
             String code,
             String privacyConsentVersion) {
     }
}
