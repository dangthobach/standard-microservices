package com.enterprise.iam.entity;

import com.enterprise.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Permission Entity
 *
 * Extends SoftDeletableEntity which provides:
 * - id (UUID)
 * - createdBy, createdAt (auto-populated from JWT on INSERT)
 * - updatedBy, updatedAt (auto-populated from JWT on UPDATE)
 * - deleted, deletedBy, deletedAt (soft delete support)
 *
 * Benefits:
 * - Automatic audit trail (who created/modified permissions)
 * - Soft delete (permissions can be deactivated and restored)
 * - Consistent with other entities in the system
 *
 * Permission Code Format:
 * - Pattern: RESOURCE:ACTION
 * - Examples: USER:READ, USER:WRITE, ORGANIZATION:DELETE
 */
@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permission_code", columnList = "code"),
        @Index(name = "idx_permission_resource_action", columnList = "resource, action"),
        @Index(name = "idx_permission_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends SoftDeletableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String resource;

    @Column(length = 50)
    private String action;

    @Builder
    public Permission(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
                      boolean deleted, String deletedBy, Instant deletedAt,
                      String code, String description, String resource, String action) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.code = code;
        this.description = description;
        this.resource = resource;
        this.action = action;
    }

    /**
     * Check if this permission matches resource and action
     */
    public boolean matches(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }

    /**
     * Get formatted permission string
     */
    public String getFormattedPermission() {
        return resource + ":" + action;
    }
}
