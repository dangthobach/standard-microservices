package com.enterprise.common.feign.error;

/**
 * Exception for HTTP 401 Unauthorized errors from Feign clients.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public class FeignUnauthorizedException extends FeignClientException {

    public FeignUnauthorizedException(String message) {
        super(401, message);
    }

    public FeignUnauthorizedException(String message, Throwable cause) {
        super(401, message, cause);
    }
}
