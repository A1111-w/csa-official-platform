package com.csa.official.common.exception;

// 继承自 CsaException，默认 404
public class ResourceNotFoundException extends CsaException {
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}