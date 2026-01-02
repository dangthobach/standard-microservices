package com.enterprise.common.feign.auth;

import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * API Key Authentication Interceptor for Feign clients.
 * <p>
 * Adds an API key to the request headers for third-party API authentication.
 * Common use cases:
 * - External REST API integration
 * - Third-party service authentication
 * - Static API key-based security
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @Bean
 * public AuthenticationInterceptor apiKeyInterceptor() {
 *     return new ApiKeyInterceptor("X-API-Key", "your-secret-key");
 * }
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
public class ApiKeyInterceptor implements AuthenticationInterceptor {

    private final String headerName;
    private final String apiKey;

    /**
     * Creates a new API key interceptor.
     *
     * @param headerName Name of the header to add (e.g., "X-API-Key", "Authorization")
     * @param apiKey     The API key value
     */
    public ApiKeyInterceptor(String headerName, String apiKey) {
        this.headerName = headerName;
        this.apiKey = apiKey;
        log.debug("ApiKeyInterceptor initialized with header: {}", headerName);
    }

    /**
     * Applies the API key to the request.
     *
     * @param template The request template to modify
     */
    @Override
    public void apply(RequestTemplate template) {
        template.header(headerName, apiKey);
        log.trace("Applied API key to header: {}", headerName);
    }
}
