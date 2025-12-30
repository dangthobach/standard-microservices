package com.enterprise.common.feign.auth;

import feign.RequestInterceptor;

/**
 * Base interface for Feign authentication interceptors.
 * <p>
 * All authentication strategies (API Key, Basic Auth, JWT, etc.) should implement this interface.
 * This provides a consistent contract for adding authentication headers to Feign requests.
 * <p>
 * Usage:
 * <pre>
 * {@code @Configuration}
 * public class MyFeignConfig {
 *     {@code @Bean}
 *     public AuthenticationInterceptor authInterceptor() {
 *         return new ApiKeyInterceptor("X-API-Key", "my-secret-key");
 *     }
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public interface AuthenticationInterceptor extends RequestInterceptor {

    /**
     * Returns the authentication type/strategy name.
     * <p>
     * This is useful for logging and debugging purposes.
     *
     * @return authentication type (e.g., "API_KEY", "BASIC_AUTH", "JWT")
     */
    default String getAuthenticationType() {
        return this.getClass().getSimpleName();
    }
}
