package com.csa.official.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class AuthCookieService {

    private final String authCookieName;
    private final String csrfCookieName;
    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(
            @Value("${csa.security.cookie.name:CSA_AUTH_TOKEN}") String authCookieName,
            @Value("${csa.security.csrf.cookie-name:CSA_CSRF_TOKEN}") String csrfCookieName,
            @Value("${csa.security.cookie.secure:false}") boolean secure,
            @Value("${csa.security.cookie.same-site:Lax}") String sameSite) {
        this.authCookieName = authCookieName;
        this.csrfCookieName = csrfCookieName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void issueAuth(HttpServletResponse response, String token, Duration maxAge) {
        addCookie(response, authCookieName, token, true, maxAge);
    }

    public void issueCsrf(HttpServletResponse response, String token, Duration maxAge) {
        addCookie(response, csrfCookieName, token, false, maxAge);
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, authCookieName, "", true, Duration.ZERO);
        addCookie(response, csrfCookieName, "", false, Duration.ZERO);
    }

    public String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authCookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, String name, String value,
                           boolean httpOnly, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
