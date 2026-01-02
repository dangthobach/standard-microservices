package com.enterprise.gateway.feign.error;

/**
 * Exception for HTTP 429 Too Many Requests errors from Feign clients.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public class FeignRateLimitException extends FeignClientException {

    public FeignRateLimitException(String message) {
        super(429, message);
    }

    public FeignRateLimitException(String message, Throwable cause) {
        super(429, message, cause);
    }
}
