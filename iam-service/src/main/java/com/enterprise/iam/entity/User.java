package com.enterprise.iam.entity;

import com.enterprise.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User Entity
 *
 * Extends SoftDeletableEntity which provides:
 * - id (UUID)
 * - createdBy, createdAt (auto-populated from JWT on INSERT)
 * - updatedBy, updatedAt (auto-populated from JWT on UPDATE)
 * - deleted, deletedBy, deletedAt (soft delete support)
 *
 * Benefits:
 * - Automatic audit trail (who created, who modified)
 * - Soft delete (users never truly deleted, can be restored)
 * - Consistent with other entities in the system
 *
 * Usage:
 * <pre>
 * // Soft delete a user
 * user.softDelete("admin@example.com");
 * userRepository.save(user);
 *
 * // Restore a user
 * user.restore();
 * userRepository.save(user);
 *
 * // Query only active users
 * List<User> activeUsers = userRepository.findAll()
 *     .stream()
 *     .filter(User::isActive)
 *     .toList();
 * </pre>
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_keycloak_id", columnList = "keycloakId"),
        @Index(name = "idx_user_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends SoftDeletableEntity<UUID> {

    @Column(nullable = false, unique = true)
    private String keycloakId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column
    private Instant lastLoginAt;

    /**
     * Builder for User entity
     * Note: Use Lombok @Builder on fields instead of class level
     * to work properly with inheritance
     */
    @Builder
    public User(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
                boolean deleted, String deletedBy, Instant deletedAt,
                String keycloakId, String email, String firstName, String lastName,
                Boolean enabled, Boolean emailVerified, Set<Role> roles, Instant lastLoginAt) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.keycloakId = keycloakId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled != null ? enabled : true;
        this.emailVerified = emailVerified != null ? emailVerified : false;
        this.roles = roles != null ? roles : new HashSet<>();
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Get full name of user
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }
}
