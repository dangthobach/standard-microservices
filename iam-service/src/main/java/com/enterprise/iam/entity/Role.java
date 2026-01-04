package com.enterprise.iam.entity;

import com.enterprise.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Role Entity
 *
 * Extends SoftDeletableEntity which provides:
 * - id (UUID)
 * - createdBy, createdAt (auto-populated from JWT on INSERT)
 * - updatedBy, updatedAt (auto-populated from JWT on UPDATE)
 * - deleted, deletedBy, deletedAt (soft delete support)
 *
 * Benefits:
 * - Automatic audit trail (who created/modified roles)
 * - Soft delete (roles can be deactivated and restored)
 * - Consistent with other entities in the system
 */
@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_role_name", columnList = "name"),
        @Index(name = "idx_role_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends SoftDeletableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @Builder
    public Role(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
                boolean deleted, String deletedBy, Instant deletedAt,
                String name, String description, Set<Permission> permissions) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.name = name;
        this.description = description;
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    /**
     * Add permission to role
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Remove permission from role
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /**
     * Check if role has specific permission
     */
    public boolean hasPermission(String permissionCode) {
        return permissions.stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }
}
