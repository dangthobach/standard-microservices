package com.enterprise.business.client;

import com.enterprise.common.feign.FeignClientConfiguration;
import com.enterprise.common.feign.auth.AuthenticationInterceptor;
import com.enterprise.common.feign.auth.JwtForwardingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Feign Configuration for IAM Service Client
 * <p>
 * Demonstrates:
 * - Importing common FeignClientConfiguration for base setup
 * - Adding JwtForwardingInterceptor for authentication
 * <p>
 * The JWT token is extracted from Spring Security context and forwarded
 * to the IAM service, maintaining the security chain.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Configuration
@Import(FeignClientConfiguration.class)  // Import base configuration
public class IamServiceFeignConfiguration {

    /**
     * Configure JWT forwarding for IAM service calls.
     * <p>
     * This interceptor extracts the JWT from the current request's
     * SecurityContextHolder and adds it to the "Authorization: Bearer {token}" header.
     *
     * @return JWT forwarding interceptor
     */
    @Bean
    public AuthenticationInterceptor jwtForwardingInterceptor() {
        return new JwtForwardingInterceptor();
    }
}
