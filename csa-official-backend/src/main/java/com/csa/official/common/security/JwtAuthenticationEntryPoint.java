package com.csa.official.common.security;

import com.csa.official.common.exception.ApiErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityResponseWriter securityResponseWriter;

    public JwtAuthenticationEntryPoint(SecurityResponseWriter securityResponseWriter) {
        this.securityResponseWriter = securityResponseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        securityResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                ApiErrorCode.AUTHENTICATION_REQUIRED, "未登录或Token无效");
    }
}
