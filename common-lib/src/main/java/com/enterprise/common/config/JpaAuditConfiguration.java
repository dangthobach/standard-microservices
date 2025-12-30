package com.enterprise.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * JPA Auditing Configuration
 *
 * Automatically populates audit fields in entities that extend AuditableEntity:
 * - createdBy: Username from JWT token on INSERT
 * - updatedBy: Username from JWT token on UPDATE
 * - createdAt: Timestamp on INSERT
 * - updatedAt: Timestamp on UPDATE
 *
 * How it works:
 * 1. Extracts JWT token from Spring Security context
 * 2. Reads "preferred_username" claim (Keycloak standard)
 * 3. Falls back to "sub" (subject) if preferred_username not found
 * 4. Uses "system" for background tasks without authentication
 *
 * Usage:
 * <pre>
 * @Entity
 * public class User extends AuditableEntity<UUID> {
 *     // Your fields here
 *     // createdBy, updatedBy auto-populated from JWT
 * }
 * </pre>
 *
 * Example JWT Claims:
 * <pre>
 * {
 *   "sub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
 *   "preferred_username": "john.doe@example.com",
 *   "email": "john.doe@example.com",
 *   "name": "John Doe"
 * }
 * </pre>
 *
 * Extracted auditor: "john.doe@example.com"
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfiguration {

    /**
     * Provides the current auditor (username) from JWT token
     *
     * Resolution order:
     * 1. JWT "preferred_username" claim (Keycloak default)
     * 2. JWT "sub" claim (subject/user ID)
     * 3. Authentication name (fallback)
     * 4. "system" (for unauthenticated operations)
     *
     * @return AuditorAware that extracts username from security context
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(this::extractUsername)
            .or(() -> Optional.of("system")); // Fallback for background tasks
    }

    /**
     * Extract username from Authentication object
     *
     * Supports:
     * - JwtAuthenticationToken (OAuth2 Resource Server)
     * - UsernamePasswordAuthenticationToken
     * - Other authentication types
     *
     * @param authentication Spring Security authentication
     * @return Username extracted from JWT or authentication name
     */
    private String extractUsername(Authentication authentication) {
        // If JWT token authentication
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            // Try preferred_username (Keycloak standard)
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }

            // Try email
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }

            // Fallback to subject (user ID)
            return jwt.getSubject();
        }

        // Fallback to authentication name
        return authentication.getName();
    }
}
