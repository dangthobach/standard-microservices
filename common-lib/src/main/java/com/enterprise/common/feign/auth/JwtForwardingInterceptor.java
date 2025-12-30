package com.enterprise.common.feign.auth;

import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Feign interceptor for JWT token forwarding.
 * <p>
 * Extracts the JWT token from the current Spring Security context and forwards it
 * to downstream services via the "Authorization: Bearer {token}" header.
 * <p>
 * This is the standard pattern for microservice-to-microservice authentication
 * where the Gateway has already validated the JWT and set it in the SecurityContext.
 * <p>
 * Usage Example:
 * <pre>
 * {@code @Configuration}
 * public class InternalServiceFeignConfig {
 *
 *     {@code @Bean}
 *     public AuthenticationInterceptor jwtForwardingInterceptor() {
 *         return new JwtForwardingInterceptor();
 *     }
 * }
 *
 * {@code @FeignClient(
 *     name = "iam-service",
 *     configuration = InternalServiceFeignConfig.class
 * )}
 * public interface IamServiceClient {
 *     {@code @GetMapping("/api/users/{id}")}
 *     UserResponse getUser({@code @PathVariable} String id);
 * }
 * </pre>
 *
 * <p>
 * <strong>Security Context Requirements:</strong>
 * <ul>
 *   <li>Spring Security must be enabled</li>
 *   <li>OAuth2 Resource Server must be configured</li>
 *   <li>JWT must be present in SecurityContextHolder</li>
 * </ul>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
public class JwtForwardingInterceptor implements AuthenticationInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        // Extract JWT from Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.trace("No authentication found in SecurityContext for request: {} {}",
                      template.method(), template.url());
            return;
        }

        String token = extractToken(authentication);

        if (token != null && !token.isEmpty()) {
            // Only add header if not already present (allow per-request override)
            if (!template.headers().containsKey(AUTHORIZATION_HEADER)) {
                template.header(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
                log.trace("Forwarded JWT token to request: {} {}", template.method(), template.url());
            } else {
                log.trace("Authorization header already present, skipping JWT forwarding for: {} {}",
                          template.method(), template.url());
            }
        } else {
            log.warn("Authentication present but JWT token is null/empty for request: {} {}",
                     template.method(), template.url());
        }
    }

    /**
     * Extracts the JWT token string from various Authentication types.
     *
     * @param authentication the Spring Security Authentication object
     * @return JWT token string, or null if not found
     */
    private String extractToken(Authentication authentication) {
        // Case 1: JwtAuthenticationToken (standard OAuth2 Resource Server)
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getTokenValue();
        }

        // Case 2: Principal is a Jwt object directly
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }

        // Case 3: Credentials contain the token (custom setup)
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String tokenString) {
            return tokenString;
        }

        // Case 4: Check if credentials is a Jwt
        if (credentials instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }

        log.debug("Unable to extract JWT from authentication type: {}",
                  authentication.getClass().getSimpleName());
        return null;
    }

    @Override
    public String getAuthenticationType() {
        return "JWT_FORWARDING";
    }
}
