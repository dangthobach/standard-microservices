package com.enterprise.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;

/**
 * Auditable Entity with automatic tracking of who and when
 *
 * Features:
 * - Auto-populate createdBy, createdAt on INSERT
 * - Auto-populate updatedBy, updatedAt on UPDATE
 * - Uses Spring Data JPA Auditing
 *
 * Configuration Required:
 * <pre>
 * @Configuration
 * @EnableJpaAuditing
 * public class JpaConfig {
 *     @Bean
 *     public AuditorAware<String> auditorProvider() {
 *         return () -> Optional.ofNullable(SecurityContextHolder.getContext())
 *             .map(SecurityContext::getAuthentication)
 *             .filter(Authentication::isAuthenticated)
 *             .map(Authentication::getName);
 *     }
 * }
 * </pre>
 *
 * Usage:
 * <pre>
 * @Entity
 * public class Organization extends AuditableEntity<Long> {
 *     // Your fields here
 *     // createdBy, createdAt, updatedBy, updatedAt auto-populated
 * }
 * </pre>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity<ID extends Serializable> extends BaseEntity<ID> {
    private static final long serialVersionUID = 1L;

    /**
     * User who created this entity
     * Auto-populated on INSERT
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    /**
     * Timestamp when entity was created
     * Auto-populated on INSERT
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * User who last updated this entity
     * Auto-populated on UPDATE
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Timestamp when entity was last updated
     * Auto-populated on UPDATE
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, createdBy=%s, createdAt=%s, updatedBy=%s, updatedAt=%s]",
            getClass().getSimpleName(), getId(), createdBy, createdAt, updatedBy, updatedAt);
    }
}
