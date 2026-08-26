package com.csa.official.common.security;

import com.csa.official.common.exception.ApiErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityResponseWriter securityResponseWriter;

    public JwtAccessDeniedHandler(SecurityResponseWriter securityResponseWriter) {
        this.securityResponseWriter = securityResponseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        securityResponseWriter.write(response, HttpStatus.FORBIDDEN.value(),
                ApiErrorCode.ACCESS_DENIED, "无权访问");
    }
}
