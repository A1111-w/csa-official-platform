package com.csa.official.common.exception;

import lombok.Getter;

@Getter
public enum ApiErrorCode {
    BAD_REQUEST(400),
    VALIDATION_FAILED(400),
    MISSING_PARAMETER(400),
    TYPE_MISMATCH(400),
    MALFORMED_REQUEST(400),
    BUSINESS_RULE_VIOLATION(400),
    AUTHENTICATION_REQUIRED(401),
    AUTHENTICATION_FAILED(401),
    ACCESS_DENIED(403),
    CSRF_INVALID(403),
    RESOURCE_NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    PAYLOAD_TOO_LARGE(413),
    RATE_LIMITED(429),
    UPSTREAM_ERROR(502),
    SERVICE_UNAVAILABLE(503),
    DATABASE_ERROR(500),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ApiErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public static ApiErrorCode fromHttpStatus(Integer status) {
        if (status == null) {
            return INTERNAL_ERROR;
        }

        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> AUTHENTICATION_REQUIRED;
            case 403 -> ACCESS_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 429 -> RATE_LIMITED;
            case 502 -> UPSTREAM_ERROR;
            case 503 -> SERVICE_UNAVAILABLE;
            default -> INTERNAL_ERROR;
        };
    }
}
