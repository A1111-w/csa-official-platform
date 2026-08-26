package com.csa.official.config;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.security.CsrfTokenService;
import com.csa.official.common.security.SecurityResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final CsrfTokenService csrfTokenService;
    private final SecurityResponseWriter securityResponseWriter;

    @Value("${csa.security.csrf.enabled:true}")
    private boolean csrfEnabled;

    @Value("${csa.security.csrf.cookie-name:CSA_CSRF_TOKEN}")
    private String csrfCookieName;

    @Value("${csa.security.csrf.header-name:X-CSRF-Token}")
    private String csrfHeaderName;

    public CsrfProtectionFilter(CsrfTokenService csrfTokenService,
                                SecurityResponseWriter securityResponseWriter) {
        this.csrfTokenService = csrfTokenService;
        this.securityResponseWriter = securityResponseWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (shouldValidate(request) && !hasValidCsrfToken(request)) {
            securityResponseWriter.write(response, HttpStatus.FORBIDDEN.value(),
                    ApiErrorCode.CSRF_INVALID, "CSRF token missing or invalid");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldValidate(HttpServletRequest request) {
        if (!csrfEnabled || SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        if (isCsrfExempt(path) || hasBearerToken(request)) {
            return false;
        }

        Object tokenSource = request.getAttribute(JwtAuthenticationFilter.AUTH_TOKEN_SOURCE_ATTRIBUTE);
        return JwtAuthenticationFilter.TOKEN_SOURCE_COOKIE.equals(tokenSource);
    }

    private boolean isCsrfExempt(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/send-code")
                || path.equals("/api/auth/csrf")
                || path.startsWith("/api/public/");
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ");
    }

    private boolean hasValidCsrfToken(HttpServletRequest request) {
        String csrfCookie = readCookie(request, csrfCookieName);
        String csrfHeader = request.getHeader(csrfHeaderName);
        return csrfTokenService.matches(csrfCookie, csrfHeader);
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
