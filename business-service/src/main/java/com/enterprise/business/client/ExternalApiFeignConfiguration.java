package com.enterprise.business.client;

import com.enterprise.common.feign.FeignClientConfiguration;
import com.enterprise.common.feign.auth.ApiKeyInterceptor;
import com.enterprise.common.feign.auth.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Feign Configuration for External API Client
 * <p>
 * Demonstrates:
 * - Importing common FeignClientConfiguration for base setup
 * - Adding ApiKeyInterceptor for third-party API authentication
 * - Reading API key from application properties
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Configuration
@Import(FeignClientConfiguration.class)  // Import base configuration
public class ExternalApiFeignConfiguration {

    /**
     * Configure API Key authentication for external API calls.
     * <p>
     * The API key is read from application.yml:
     * <pre>
     * app:
     *   services:
     *     external:
     *       api-key: your-secret-api-key
     * </pre>
     *
     * @param apiKey API key from configuration
     * @return API key interceptor
     */
    @Bean
    public AuthenticationInterceptor apiKeyInterceptor(
        @Value("${app.services.external.api-key:demo-key}") String apiKey
    ) {
        // Use custom header name if API requires it
        return new ApiKeyInterceptor("X-API-Key", apiKey);
    }
}
