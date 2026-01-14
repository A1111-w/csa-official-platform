package com.csa.official.config;

import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.sys.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头获取 Token
        // 格式通常是: "Authorization: Bearer xxxxx.yyyyy.zzzzz"
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // 2. 截取 "Bearer " 之后的部分，拿到真正的 Token
            String token = authHeader.substring(7);

            try {
                // 3. 检查 Token 是否过期/合法
                if (!jwtUtils.isTokenExpired(token)) {
                    // 4. 从 Token 里解析出用户名
                    String username = jwtUtils.getUsernameFromToken(token);

                    // 5. 确保当前上下文中没有认证信息 (避免重复认证)
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 6. 从数据库加载用户详细信息 (查库确保用户真的存在，且没被封号)
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // 7. 生成 Spring Security
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // Token 解析失败（比如被篡改、过期），这里暂时不抛异常，直接放行
                // 后面的 Security 拦截器发现 SecurityContext 里没票据，自然会报 403
                logger.error("Token 验证失败: {}", e);
            }
        }

        // 9. 继续执行下一个过滤器 (放行)
        filterChain.doFilter(request, response);
    }
}