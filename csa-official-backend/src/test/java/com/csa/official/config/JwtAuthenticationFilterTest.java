package com.csa.official.config;

import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.security.JwtRevocationService;
import com.csa.official.common.security.LoginUser;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsTokenWhoseSessionVersionIsStale() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        JwtRevocationService revocationService = mock(JwtRevocationService.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        FilterChain filterChain = mock(FilterChain.class);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "jwtRevocationService", revocationService);
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(filter, "authCookieName", "CSA_AUTH_TOKEN");

        User user = new User();
        user.setId(42L);
        user.setUsername("member");
        user.setPassword("stored-hash");
        user.setRoleLevel(1);
        user.setSessionVersion(3L);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDeleted(0);

        when(jwtUtils.isTokenExpired("stale-session-token")).thenReturn(false);
        when(revocationService.isRevoked("stale-session-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("stale-session-token")).thenReturn("member");
        when(jwtUtils.getSessionVersionFromToken("stale-session-token")).thenReturn(2L);
        when(userDetailsService.loadUserByUsername("member")).thenReturn(new LoginUser(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer stale-session-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rejectsLegacyTokenWithoutSessionVersion() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        JwtRevocationService revocationService = mock(JwtRevocationService.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        FilterChain filterChain = mock(FilterChain.class);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "jwtRevocationService", revocationService);
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(filter, "authCookieName", "CSA_AUTH_TOKEN");

        User user = new User();
        user.setId(42L);
        user.setUsername("member");
        user.setPassword("stored-hash");
        user.setRoleLevel(1);
        user.setSessionVersion(0L);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDeleted(0);

        when(jwtUtils.isTokenExpired("legacy-token")).thenReturn(false);
        when(revocationService.isRevoked("legacy-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("legacy-token")).thenReturn("member");
        when(jwtUtils.getSessionVersionFromToken("legacy-token")).thenReturn(null);
        when(userDetailsService.loadUserByUsername("member")).thenReturn(new LoginUser(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer legacy-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
