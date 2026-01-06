package com.enterprise.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User Role Service (Gateway)
 * <p>
 * Fetches and caches user roles from IAM Service.
 * Used for role-based authorization (e.g., dashboard access).
 * <p>
 * Caching Strategy:
 * - L1 Cache: Caffeine (in-memory, 60s TTL, 100K max entries)
 * - L2 Cache: Redis (distributed, 1h TTL)
 * - Fallback: HTTP call to IAM Service
 * <p>
 * Why roles are fetched from IAM instead of JWT:
 * - JWT tokens in this architecture do NOT contain role claims
 * - Roles are managed dynamically in IAM database
 * - Decouples authorization from identity provider (Keycloak)
 * <p>
 * Uses LoadBalanced WebClient for service discovery and load balancing
 * across multiple IAM service instances.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserRoleService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient webClient;
    private final Cache<String, List<String>> roleCache;

    private static final String ROLE_KEY_PREFIX = "authz:roles:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);
    private static final Duration L1_TTL_SECONDS = Duration.ofSeconds(60);
    private static final int L1_MAX_SIZE = 100_000;

    /**
     * Constructor with LoadBalanced WebClient.Builder for service discovery.
     *
     * @param redisTemplate Redis template for L2 cache
     * @param loadBalancedWebClientBuilder LoadBalanced WebClient builder
     * @param iamServiceName Service name (default: "iam-service")
     */
    public UserRoleService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @LoadBalanced WebClient.Builder loadBalancedWebClientBuilder,
            @Value("${iam.service.name:iam-service}") String iamServiceName) {
        this.redisTemplate = redisTemplate;
        // Use lb:// scheme for LoadBalancer to resolve service name from Consul
        String serviceUrl = "lb://" + iamServiceName;
        this.webClient = loadBalancedWebClientBuilder.baseUrl(serviceUrl).build();

        // L1 Cache: Caffeine with 60s TTL and 100K max entries
        this.roleCache = Caffeine.newBuilder()
                .expireAfterWrite(L1_TTL_SECONDS.toSeconds(), TimeUnit.SECONDS)
                .maximumSize(L1_MAX_SIZE)
                .recordStats()
                .build();

        log.info("✅ UserRoleService initialized with LoadBalanced WebClient for service: {}", serviceUrl);
        log.info("UserRoleService initialized with L1 cache ({}s TTL, {} max) and L2 cache ({}h TTL)",
                L1_TTL_SECONDS.toSeconds(), L1_MAX_SIZE, REDIS_TTL.toHours());
    }

    /**
     * Get roles for a user by Keycloak ID.
     * <p>
     * Flow: L1 Cache -> L2 Cache (Redis) -> IAM Service
     *
     * @param keycloakId Keycloak user ID (sub claim from JWT)
     * @return Mono of role names list
     */
    public Mono<List<String>> getRoles(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            log.warn("getRoles called with null or blank keycloakId");
            return Mono.just(Collections.emptyList());
        }

        // 1. Check L1 Cache (Caffeine - in-memory)
        List<String> cachedRoles = roleCache.getIfPresent(keycloakId);
        if (cachedRoles != null) {
            log.debug("L1 Cache HIT for keycloakId: {}", keycloakId);
            return Mono.just(cachedRoles);
        }

        // 2. Check L2 Cache (Redis)
        String redisKey = ROLE_KEY_PREFIX + keycloakId;
        return redisTemplate.opsForList().range(redisKey, 0, -1)
                .collectList()
                .flatMap(redisRoles -> {
                    if (!redisRoles.isEmpty()) {
                        // L2 Cache HIT -> Populate L1
                        log.debug("L2 Cache HIT for keycloakId: {}, roles: {}", keycloakId, redisRoles);
                        roleCache.put(keycloakId, redisRoles);
                        return Mono.just(redisRoles);
                    }

                    // 3. Cache MISS -> Fetch from IAM Service
                    log.debug("Cache MISS for keycloakId: {}, fetching from IAM", keycloakId);
                    return fetchRolesFromIam(keycloakId);
                });
    }

    /**
     * Check if user has any of the specified roles.
     *
     * @param keycloakId    Keycloak user ID
     * @param allowedRoles  List of roles to check against
     * @return Mono<Boolean> true if user has ANY of the allowed roles
     */
    public Mono<Boolean> hasAnyRole(String keycloakId, List<String> allowedRoles) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return Mono.just(false);
        }

        return getRoles(keycloakId)
                .map(userRoles -> userRoles.stream()
                        .anyMatch(allowedRoles::contains));
    }

    /**
     * Fetch roles from IAM Service and populate both caches.
     */
    private Mono<List<String>> fetchRolesFromIam(String keycloakId) {
        return webClient.get()
                .uri("/api/internal/roles/keycloak/{keycloakId}", keycloakId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .doOnNext(roles -> {
                    log.debug("Fetched roles from IAM for keycloakId: {}, roles: {}", keycloakId, roles);
                    
                    // Populate L1 Cache
                    roleCache.put(keycloakId, roles);

                    // Populate L2 Cache (Redis) - Async
                    if (!roles.isEmpty()) {
                        String redisKey = ROLE_KEY_PREFIX + keycloakId;
                        redisTemplate.delete(redisKey)
                                .then(redisTemplate.opsForList().rightPushAll(redisKey, roles))
                                .then(redisTemplate.expire(redisKey, REDIS_TTL))
                                .subscribe(
                                    success -> log.debug("Cached roles in Redis for keycloakId: {}", keycloakId),
                                    error -> log.warn("Failed to cache roles in Redis: {}", error.getMessage())
                                );
                    }
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch roles from IAM for keycloakId: {}", keycloakId, e);
                    return Mono.just(Collections.emptyList());
                });
    }

    /**
     * Invalidate caches for a user (called on role change events).
     *
     * @param keycloakId Keycloak user ID
     * @return Mono<Void>
     */
    public Mono<Void> invalidateUserRoles(String keycloakId) {
        log.info("Invalidating role cache for keycloakId: {}", keycloakId);
        roleCache.invalidate(keycloakId);
        return redisTemplate.delete(ROLE_KEY_PREFIX + keycloakId).then();
    }

    /**
     * Get L1 cache statistics (for monitoring).
     */
    public String getCacheStats() {
        return roleCache.stats().toString();
    }
}
