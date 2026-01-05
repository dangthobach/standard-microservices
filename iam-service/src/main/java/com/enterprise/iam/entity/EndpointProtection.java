package com.enterprise.iam.entity;

import com.enterprise.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Endpoint Protection Entity
 * <p>
 * Defines dynamic authorization rules for API endpoints.
 * Stored in Database and synced to Gateway for enforcement.
 * <p>
 * Fields:
 * - pattern: Ant-style path pattern (e.g. /api/business/orders/**)
 * - method: HTTP Method (GET, POST, etc.)
 * - permissionCode: Required permission (e.g. ORDER:CREATE)
 * - isPublic: If true, allows access without permission check (PermitAll)
 * - priority: Evaluation order (Higher value = Higher priority)
 */
@Entity
@Table(name = "endpoint_protections", indexes = {
        @Index(name = "idx_ep_active_priority", columnList = "active, priority DESC"),
        @Index(name = "idx_ep_pattern", columnList = "pattern")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointProtection extends SoftDeletableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 200)
    private String pattern;

    @Column(nullable = false, length = 10)
    private String method; // GET, POST, PUT, DELETE, *

    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Builder
    public EndpointProtection(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
            boolean deleted, String deletedBy, Instant deletedAt,
            String pattern, String method, String permissionCode, boolean isPublic, Integer priority, boolean active) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.pattern = pattern;
        this.method = method;
        this.permissionCode = permissionCode;
        this.isPublic = isPublic;
        this.priority = priority;
        this.active = active;
    }
}
