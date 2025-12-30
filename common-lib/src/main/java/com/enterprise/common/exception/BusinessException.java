package com.enterprise.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Object metadata;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.metadata = null;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = null;
    }

    public BusinessException(String errorCode, String message, Object metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = null;
    }
}
