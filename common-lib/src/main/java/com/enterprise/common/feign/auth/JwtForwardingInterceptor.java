package com.enterprise.common.feign.auth;

import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * JWT Token Forwarding Interceptor for Feign clients.
 * <p>
 * Extracts the JWT token from the Spring Security context and forwards it
 * to downstream services in the Authorization header.
 * <p>
 * This is essential for maintaining the security chain in microservices:
 * - User authenticates with the gateway
 * - Gateway forwards JWT to service A
 * - Service A uses this interceptor to forward JWT to service B
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @Bean
 * public AuthenticationInterceptor jwtForwardingInterceptor() {
 *     return new JwtForwardingInterceptor();
 * }
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
public class JwtForwardingInterceptor implements AuthenticationInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Applies the JWT token to the request if available.
     * <p>
     * Extracts the token from SecurityContextHolder and adds it to the
     * Authorization header with "Bearer " prefix.
     *
     * @param template The request template to modify
     */
    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String tokenValue = jwtToken.getToken().getTokenValue();
            template.header(AUTHORIZATION_HEADER, BEARER_PREFIX + tokenValue);
            log.trace("Forwarded JWT token to downstream service");
        } else {
            log.debug("No JWT token found in security context, skipping token forwarding");
        }
    }
}
