package com.enterprise.iam.service;

import com.enterprise.iam.entity.User;
import com.enterprise.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * User Service with Multi-Level Caching
 * <p>
 * Implements L1 (Caffeine) caching for frequently accessed user data.
 * Critical for 1M CCU performance optimization.
 * <p>
 * Cache Strategy:
 * - findByEmail/findByKeycloakId: Cacheable (L1 + Repository)
 * - save: CachePut (update cache on save)
 * - delete: CacheEvict (remove from cache)
 * <p>
 * Cache Names:
 * - users: Cache by user ID
 * - usersByEmail: Cache by email
 * - usersByKeycloakId: Cache by Keycloak ID
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Find user by ID with caching.
     *
     * @param id User ID
     * @return Optional User
     */
    @Cacheable(value = "users", key = "#id", unless = "#result == null || !#result.isPresent()")
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        log.debug("Finding user by id: {} (cache miss)", id);
        return userRepository.findById(id);
    }

    /**
     * Find user by email with L1 caching.
     * <p>
     * This is a critical method called during authentication.
     * L1 cache reduces database load significantly under high concurrency.
     *
     * @param email User email
     * @return Optional User with roles (uses @EntityGraph to prevent N+1)
     */
    @Cacheable(value = "usersByEmail", key = "#email", unless = "#result == null || !#result.isPresent()")
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {} (cache miss)", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by Keycloak ID with L1 caching.
     * <p>
     * Called during OAuth2 authentication flow.
     * Critical for performance under high load.
     *
     * @param keycloakId Keycloak user ID
     * @return Optional User with roles (uses @EntityGraph to prevent N+1)
     */
    @Cacheable(value = "usersByKeycloakId", key = "#keycloakId", unless = "#result == null || !#result.isPresent()")
    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakId(String keycloakId) {
        log.debug("Finding user by keycloakId: {} (cache miss)", keycloakId);
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Save or update user and update cache.
     * <p>
     * Uses @CachePut to update cache entries on save.
     * Evicts email and keycloakId caches to prevent stale data.
     *
     * @param user User to save
     * @return Saved user
     */
    @CachePut(value = "users", key = "#user.id")
    @CacheEvict(value = {"usersByEmail", "usersByKeycloakId"}, key = "#user.email")
    @Transactional
    public User save(User user) {
        log.debug("Saving user: {} (updating cache)", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * Delete user (soft delete) and evict from cache.
     *
     * @param id User ID
     */
    @CacheEvict(value = {"users", "usersByEmail", "usersByKeycloakId"}, allEntries = true)
    @Transactional
    public void deleteById(UUID id) {
        log.debug("Deleting user: {} (evicting cache)", id);
        userRepository.findById(id).ifPresent(user -> {
            user.softDelete("system");
            userRepository.save(user);
        });
    }

    /**
     * Check if user exists by email.
     *
     * @param email User email
     * @return true if user exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }

    /**
     * Check if user exists by Keycloak ID.
     *
     * @param keycloakId Keycloak user ID
     * @return true if user exists
     */
    @Transactional(readOnly = true)
    public boolean existsByKeycloakId(String keycloakId) {
        return userRepository.existsByKeycloakIdAndDeletedFalse(keycloakId);
    }

    /**
     * Count enabled users.
     *
     * @return Count of enabled users
     */
    @Transactional(readOnly = true)
    public long countEnabledUsers() {
        return userRepository.countByEnabled(true);
    }
}
