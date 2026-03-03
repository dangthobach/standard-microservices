package com.enterprise.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
<<<<<<< HEAD
=======
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
>>>>>>> 020fef45ac3df722878d8fb63bf20adbb95dd5e3
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

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
 * 1. Checks if Spring Security is present in classpath
 * 2. If present, delegates to SpringSecurityAuditorAware to extract username from JWT
 * 3. If absent, uses a default auditor returning "system"
 *
 * Usage:
 * 
 * <pre>
 * @Entity
 * public class User extends AuditableEntity<UUID> {
 *     // Your fields here
 * }
 * </pre>
<<<<<<< HEAD
 *
 * Example JWT Claims:
 * 
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
=======
>>>>>>> 020fef45ac3df722878d8fb63bf20adbb95dd5e3
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
public class JpaAuditConfiguration {

<<<<<<< HEAD
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
=======
    @Bean(name = "auditorProvider")
    @ConditionalOnClass(name = "org.springframework.security.core.Authentication")
    public AuditorAware<String> securityAuditorProvider() {
        return new SpringSecurityAuditorAware();
>>>>>>> 020fef45ac3df722878d8fb63bf20adbb95dd5e3
    }

    @Bean(name = "auditorProvider")
    @ConditionalOnMissingClass("org.springframework.security.core.Authentication")
    public AuditorAware<String> defaultAuditorProvider() {
        return () -> Optional.of("system");
    }
}
