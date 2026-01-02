package com.enterprise.gateway.feign.error;

import lombok.Getter;

/**
 * Exception for Feign client errors (HTTP 4xx).
 * <p>
 * Represents client-side errors when calling downstream services via Feign.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Getter
public class FeignClientException extends RuntimeException {

    private final int statusCode;

    public FeignClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public FeignClientException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
