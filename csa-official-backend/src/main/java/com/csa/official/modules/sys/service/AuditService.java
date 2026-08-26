package com.csa.official.modules.sys.service;

import com.csa.official.common.observability.TraceContext;
import com.csa.official.common.security.LoginUser;
import com.csa.official.modules.sys.entity.AuditLog;
import com.csa.official.modules.sys.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class AuditService {

    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final Set<String> FORBIDDEN_DETAIL_KEYS = Set.of(
            "password", "newpassword", "currentpassword", "token", "secret", "cookie",
            "authorization", "credential", "verificationcode", "resetcode");

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(String action, String targetType, String targetId, Map<String, ?> details) {
        record(action, targetType, targetId, "SUCCESS", null, details);
    }

    public void record(String action, String targetType, String targetId, String result,
                       String unauthenticatedUsername, Map<String, ?> details) {
        validateDetails(details);
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setResult(result);
        entry.setRequestId(TraceContext.currentTraceId());
        entry.setCreateTime(LocalDateTime.now());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            entry.setActorUserId(loginUser.getId());
            entry.setActorUsername(loginUser.getUsername());
        } else if (StringUtils.hasText(unauthenticatedUsername)) {
            entry.setActorUsername(limit(unauthenticatedUsername, 64));
        }

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entry.setIpAddress(limit(request.getRemoteAddr(), 64));
            entry.setUserAgent(limit(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
        }
        entry.setDetailsJson(toJson(details));
        auditLogMapper.insert(entry);
    }

    public void recordBestEffort(String action, String targetType, String targetId, String result,
                                 String unauthenticatedUsername, Map<String, ?> details) {
        try {
            record(action, targetType, targetId, result, unauthenticatedUsername, details);
        } catch (RuntimeException e) {
            log.error("Audit write failed: action={}, targetType={}, requestId={}",
                    action, targetType, TraceContext.currentTraceId(), e);
        }
    }

    private void validateDetails(Map<String, ?> details) {
        validateValue(details);
    }

    private void validateValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
                String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
                if (isForbiddenDetailKey(normalized)) {
                    throw new IllegalArgumentException("Sensitive values must not be written to audit logs: " + key);
                }
                validateValue(entry.getValue());
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(this::validateValue);
            return;
        }
        if (value != null && value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                validateValue(Array.get(value, index));
            }
        }
    }

    private boolean isForbiddenDetailKey(String normalized) {
        return FORBIDDEN_DETAIL_KEYS.contains(normalized)
                || normalized.endsWith("password")
                || normalized.endsWith("token")
                || normalized.endsWith("secret")
                || normalized.endsWith("cookie")
                || normalized.endsWith("credential")
                || normalized.endsWith("verificationcode")
                || normalized.endsWith("resetcode");
    }

    private String toJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Audit details must be JSON serializable", e);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
