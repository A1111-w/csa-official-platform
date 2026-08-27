package com.csa.official.modules.sys.controller;

import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.security.AuthCookieService;
import com.csa.official.common.security.JwtRevocationService;
import com.csa.official.common.security.LoginUser;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.service.AccountService;
import com.csa.official.modules.sys.service.PersonalDataExportService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletionRequestRevokesCurrentTokenAndClearsBothCookies() {
        AccountService accountService = mock(AccountService.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);
        JwtRevocationService revocationService = mock(JwtRevocationService.class);
        PersonalDataExportService exportService = mock(PersonalDataExportService.class);
        when(authCookieService.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn("session-token");

        AccountController controller = new AccountController(accountService, authCookieService, revocationService,
                exportService);
        User user = new User();
        user.setId(42L);
        user.setUsername("member");
        user.setRoleLevel(1);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDeleted(0);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(user), null,
                        new LoginUser(user).getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("CSA_AUTH_TOKEN", "session-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.requestDeletion(request, response);

        verify(accountService).requestDeletion(user);
        verify(revocationService).revoke("session-token");
        verify(authCookieService).clear(response);
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }
}
