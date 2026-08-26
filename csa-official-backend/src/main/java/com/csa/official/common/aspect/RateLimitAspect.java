package com.csa.official.common.aspect;

import com.csa.official.common.annotation.RateLimit;
import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final KeyValueStore keyValueStore;
    private final List<IpAddressMatcher> trustedProxies;
    private final String forwardedIpHeader;
    private final AuditService auditService;

    public RateLimitAspect(
            KeyValueStore keyValueStore,
            AuditService auditService,
            @Value("${csa.security.rate-limit.trusted-proxies:}") String trustedProxies,
            @Value("${csa.security.rate-limit.forwarded-ip-header:X-Forwarded-For}") String forwardedIpHeader) {
        this.keyValueStore = keyValueStore;
        this.auditService = auditService;
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(IpAddressMatcher::new)
                .toList();
        this.forwardedIpHeader = forwardedIpHeader;
    }

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = getClientIp(request);
        String key = buildRateLimitKey(point, request, rateLimit, ip);

        Long count = keyValueStore.increment(key, rateLimit.time(), TimeUnit.SECONDS);
        if (count != null && count > rateLimit.count()) {
            auditService.recordBestEffort("RATE_LIMIT_TRIGGERED", "ENDPOINT", request.getRequestURI(),
                    "REJECTED", null, Map.of(
                            "httpMethod", request.getMethod(),
                            "rateLimitKey", rateLimit.key(),
                            "clientIp", ip,
                            "windowSeconds", rateLimit.time(),
                            "allowedCount", rateLimit.count()));
        }

        if (count != null && count > rateLimit.count()) {
            log.warn("用户触发限流, ip={}, key={}", ip, key);
            throw new CsaException(429, "操作过于频繁，请 " + rateLimit.time() + " 秒后再试");
        }
    }

    private String buildRateLimitKey(JoinPoint point, HttpServletRequest request, RateLimit rateLimit, String ip) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:")
                .append(rateLimit.key())
                .append(":ip:")
                .append(ip);

        List<String> identifierParts = resolveIdentifierParts(point, request, rateLimit.identifiers());
        if (!identifierParts.isEmpty()) {
            keyBuilder.append(":biz:").append(String.join("|", identifierParts));
        }
        return keyBuilder.toString();
    }

    private List<String> resolveIdentifierParts(JoinPoint point, HttpServletRequest request, String[] identifiers) {
        List<String> parts = new ArrayList<>();
        if (identifiers == null || identifiers.length == 0) {
            return parts;
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = point.getArgs();

        for (String identifier : identifiers) {
            String value = resolveIdentifierValue(identifier, request, parameterNames, args);
            if (value != null) {
                parts.add(identifier + "=" + value);
            }
        }
        return parts;
    }

    private String resolveIdentifierValue(String identifier, HttpServletRequest request, String[] parameterNames, Object[] args) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        String requestParameter = normalizeIdentifierValue(request.getParameter(identifier));
        if (requestParameter != null) {
            return requestParameter;
        }

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }

            if (arg instanceof Map<?, ?> map) {
                Object mapValue = map.get(identifier);
                String normalizedMapValue = normalizeIdentifierValue(Objects.toString(mapValue, null));
                if (normalizedMapValue != null) {
                    return normalizedMapValue;
                }
            }

            if (parameterNames != null && i < parameterNames.length && identifier.equals(parameterNames[i])) {
                String parameterValue = normalizeIdentifierValue(Objects.toString(arg, null));
                if (parameterValue != null) {
                    return parameterValue;
                }
            }

            try {
                BeanWrapper beanWrapper = new BeanWrapperImpl(arg);
                if (beanWrapper.isReadableProperty(identifier)) {
                    Object propertyValue = beanWrapper.getPropertyValue(identifier);
                    String normalizedPropertyValue = normalizeIdentifierValue(Objects.toString(propertyValue, null));
                    if (normalizedPropertyValue != null) {
                        return normalizedPropertyValue;
                    }
                }
            } catch (Exception ignored) {
                // Ignore non-bean arguments and continue scanning for configured identifiers.
            }
        }

        return null;
    }

    private String normalizeIdentifierValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized
                .replace(':', '_')
                .replace('|', '_')
                .toLowerCase(Locale.ROOT);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = normalizeLoopback(request.getRemoteAddr());
        if (trustedProxies.stream().noneMatch(matcher -> matcher.matches(remoteAddr))) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader(forwardedIpHeader);
        if (!StringUtils.hasText(forwardedFor)) {
            return remoteAddr;
        }

        return Arrays.stream(forwardedFor.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(ip -> !"unknown".equalsIgnoreCase(ip))
                .map(this::normalizeLoopback)
                .findFirst()
                .orElse(remoteAddr);
    }

    private String normalizeLoopback(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
