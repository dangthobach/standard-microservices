package com.enterprise.common.feign.auth;

import feign.RequestInterceptor;

/**
 * Base interface for Feign authentication interceptors.
 * <p>
 * All authentication interceptors should implement this interface
 * to ensure consistent handling across different authentication methods.
 * <p>
 * Common implementations:
 * - {@link ApiKeyInterceptor} - API key authentication
 * - {@link JwtForwardingInterceptor} - JWT token forwarding
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public interface AuthenticationInterceptor extends RequestInterceptor {
    // Marker interface extending RequestInterceptor
    // Provides type safety and clarity for authentication-specific interceptors
}
