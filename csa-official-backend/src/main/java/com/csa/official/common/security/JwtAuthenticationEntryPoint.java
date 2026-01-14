package com.csa.official.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证失败处理器
 * 当用户未登录、Token过期或无效时，Spring Security 会调用这个类
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        // 1. 设置响应头
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 状态码

        // 2. 构建 JSON 结果 (手动构建，不依赖 R 类，防止循环依赖或序列化问题)
        Map<String, Object> map = new HashMap<>();
        map.put("code", 401);
        map.put("message", "认证失败：登录已过期或Token无效");
        map.put("data", null);

        // 3. 写出 JSON
        new ObjectMapper().writeValue(response.getOutputStream(), map);
    }
}