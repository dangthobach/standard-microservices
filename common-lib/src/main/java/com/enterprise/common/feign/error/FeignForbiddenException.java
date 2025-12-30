package com.enterprise.common.feign.error;

/**
 * Exception for HTTP 403 Forbidden errors from Feign clients.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public class FeignForbiddenException extends FeignClientException {

    public FeignForbiddenException(String message) {
        super(403, message);
    }

    public FeignForbiddenException(String message, Throwable cause) {
        super(403, message, cause);
    }
}
