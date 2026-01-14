package com.csa.official.common.exception;

import lombok.Getter;

@Getter
public class CsaException extends RuntimeException {
    private Integer code;

    // 1. 单参数构造方法 (默认 500)
    public CsaException(String message) {
        super(message);
        this.code = 500;
    }

    public CsaException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}