package com.enterprise.iam.repository;

import com.enterprise.iam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 * <p>
 * JPA repository for User entity operations.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email.
     *
     * @param email Email address
     * @return Optional User
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by Keycloak ID.
     *
     * @param keycloakId Keycloak user ID
     * @return Optional User
     */
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
}
