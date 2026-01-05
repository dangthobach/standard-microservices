package com.enterprise.iam.repository;

import com.enterprise.iam.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 * <p>
 * JPA repository for User entity operations.
 * Uses @EntityGraph to prevent N+1 query problems when fetching user roles.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email with roles eagerly fetched.
     * <p>
     * Uses @EntityGraph to fetch roles in a single query, preventing N+1 problem.
     * Critical for high-load scenarios (1M CCU).
     *
     * @param email Email address
     * @return Optional User with roles
     */
    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findByEmail(String email);

    /**
     * Find user by Keycloak ID with roles eagerly fetched.
     * <p>
     * Uses @EntityGraph to fetch roles in a single query, preventing N+1 problem.
     * Critical for authentication flow under high load.
     *
     * @param keycloakId Keycloak user ID
     * @return Optional User with roles
     */
    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Count users by enabled status.
     *
     * @param enabled Enabled status
     * @return Count of users
     */
    long countByEnabled(Boolean enabled);

    /**
     * Check if user exists by email.
     *
     * @param email Email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if active (non-deleted) user exists by email.
     *
     * @param email Email address
     * @return true if active user exists
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Check if active (non-deleted) user exists by Keycloak ID.
     *
     * @param keycloakId Keycloak user ID
     * @return true if active user exists
     */
    boolean existsByKeycloakIdAndDeletedFalse(String keycloakId);

    /**
     * Find all permission codes for a user.
     * Joins User -> Role -> Permission to get distinct permission codes.
     * <p>
     * Used by Gateway to cache User Permissions.
     *
     * @param userId User ID
     * @return List of permission codes (e.g. "ORDER:CREATE")
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.code FROM User u JOIN u.roles r JOIN r.permissions p WHERE u.id = :userId AND u.deleted = false AND r.deleted = false AND p.deleted = false")
    java.util.List<String> findPermissionCodesByUserId(UUID userId);
}
