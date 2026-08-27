package com.csa.official.common.exception;

import lombok.Getter;

@Getter
public class CsaException extends RuntimeException {
    private final Integer code;
    private final String errorCode;

    public CsaException(String message) {
        this(ApiErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public CsaException(Integer code, String message) {
        this(code, ApiErrorCode.fromHttpStatus(code).name(), message, null);
    }

    public CsaException(ApiErrorCode errorCode, String message) {
        this(errorCode.getHttpStatus(), errorCode.name(), message, null);
    }

    public CsaException(ApiErrorCode errorCode, String message, Throwable cause) {
        this(errorCode.getHttpStatus(), errorCode.name(), message, cause);
    }

    public CsaException(Integer code, String errorCode, String message) {
        this(code, errorCode, message, null);
    }

    private CsaException(Integer code, String errorCode, String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        this.code = code == null ? 500 : code;
        this.errorCode = errorCode == null || errorCode.isBlank()
                ? ApiErrorCode.fromHttpStatus(this.code).name()
                : errorCode;
    }
}
