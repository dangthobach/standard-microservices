package com.enterprise.gateway.feign.error;

/**
 * Exception for HTTP 404 Not Found errors from Feign clients.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public class FeignNotFoundException extends FeignClientException {

    public FeignNotFoundException(String message) {
        super(404, message);
    }

    public FeignNotFoundException(String message, Throwable cause) {
        super(404, message, cause);
    }
}
