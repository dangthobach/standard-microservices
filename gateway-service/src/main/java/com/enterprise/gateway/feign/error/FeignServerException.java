package com.enterprise.gateway.feign.error;

import lombok.Getter;

/**
 * Exception for Feign server errors (HTTP 5xx).
 * <p>
 * Represents server-side errors in downstream services called via Feign.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Getter
public class FeignServerException extends RuntimeException {

    private final int statusCode;

    public FeignServerException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public FeignServerException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
