package com.csa.official.config;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.security.SecurityResponseWriter;
import com.csa.official.common.security.JwtRevocationService;
import com.csa.official.common.util.JwtUtils;
import com.csa.official.modules.sys.service.UserDetailsServiceImpl;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_TOKEN_SOURCE_ATTRIBUTE = "csa.authTokenSource";
    public static final String TOKEN_SOURCE_BEARER = "bearer";
    public static final String TOKEN_SOURCE_COOKIE = "cookie";

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JwtRevocationService jwtRevocationService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private SecurityResponseWriter securityResponseWriter;

    @Value("${csa.security.cookie.name:CSA_AUTH_TOKEN}")
    private String authCookieName;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        ResolvedToken resolvedToken = resolveToken(request);
        String token = resolvedToken.token();

        if (StringUtils.hasText(token)) {
            try {
                if (!jwtUtils.isTokenExpired(token) && !jwtRevocationService.isRevoked(token)) {
                    String username = jwtUtils.getUsernameFromToken(token);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        Long tokenSessionVersion = jwtUtils.getSessionVersionFromToken(token);
                        Long currentSessionVersion = ((com.csa.official.common.security.LoginUser) userDetails)
                                .getUser().getSessionVersion();
                        boolean sessionIsCurrent = tokenSessionVersion != null
                                && Objects.equals(tokenSessionVersion,
                                currentSessionVersion == null ? 0L : currentSessionVersion);

                        if (userDetails.isEnabled() && sessionIsCurrent) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            request.setAttribute(AUTH_TOKEN_SOURCE_ATTRIBUTE, resolvedToken.source());
                        } else {
                            SecurityContextHolder.clearContext();
                        }
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                logger.debug("Token 验证失败，已按未认证处理", e);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                logger.error("鉴权依赖访问失败", e);
                securityResponseWriter.write(response, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ApiErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后再试");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private ResolvedToken resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return new ResolvedToken(authHeader.substring(7), TOKEN_SOURCE_BEARER);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResolvedToken.empty();
        }

        for (Cookie cookie : cookies) {
            if (authCookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return new ResolvedToken(cookie.getValue(), TOKEN_SOURCE_COOKIE);
            }
        }

        return ResolvedToken.empty();
    }

    private record ResolvedToken(String token, String source) {
        private static ResolvedToken empty() {
            return new ResolvedToken(null, null);
        }
    }
}
