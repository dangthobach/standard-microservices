package com.enterprise.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
 * <pre>
 * @Entity
 * public class User extends AuditableEntity<UUID> {
 *     // Your fields here
 * }
 * </pre>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfiguration {

    @Bean(name = "auditorProvider")
    @ConditionalOnClass(name = "org.springframework.security.core.Authentication")
    public AuditorAware<String> securityAuditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    @Bean(name = "auditorProvider")
    @ConditionalOnMissingClass("org.springframework.security.core.Authentication")
    public AuditorAware<String> defaultAuditorProvider() {
        return () -> Optional.of("system");
    }
}
