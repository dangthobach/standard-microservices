package com.enterprise.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Authorization Service (Gateway)
 * <p>
 * Handles Permission Caching for Centralized AuthZ.
 * Strategy: L1 (Caffeine) -> L2 (Redis) -> IAM Service (Source)
 * <p>
 * Uses LoadBalanced WebClient for service discovery and load balancing
 * across multiple IAM service instances.
 */
@Slf4j
@Service
public class AuthZService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient webClient;
    private final Cache<String, List<String>> permissionCache;

    private static final String PERMISSION_KEY_PREFIX = "authz:perms:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    /**
     * Constructor with LoadBalanced WebClient.Builder for service discovery.
     *
     * @param redisTemplate Redis template for L2 cache
     * @param loadBalancedWebClientBuilder LoadBalanced WebClient builder
     * @param iamServiceName Service name (default: "iam-service")
     */
    public AuthZService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @LoadBalanced WebClient.Builder loadBalancedWebClientBuilder,
            @Value("${iam.service.name:iam-service}") String iamServiceName) {
        this.redisTemplate = redisTemplate;
        // Use lb:// scheme for LoadBalancer to resolve service name from Consul
        String serviceUrl = "lb://" + iamServiceName;
        this.webClient = loadBalancedWebClientBuilder.baseUrl(serviceUrl).build();
        log.info("✅ AuthZService configured with LoadBalanced WebClient for service: {}", serviceUrl);

        // L1 Cache: 60 seconds TTL, Max 100K users
        this.permissionCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(100_000)
                .recordStats()
                .build();
    }

    /**
     * Check if user has required permission.
     *
     * @param userId             User ID
     * @param requiredPermission Permission code
     * @return true if allowed
     */
    public Mono<Boolean> hasPermission(String userId, String requiredPermission) {
        if (userId == null || requiredPermission == null)
            return Mono.just(false);

        // 1. Check L1 Cache
        List<String> cachedPerms = permissionCache.getIfPresent(userId);
        if (cachedPerms != null) {
            return Mono.just(cachedPerms.contains(requiredPermission));
        }

        // 2. Check L2 Cache (Redis)
        String redisKey = PERMISSION_KEY_PREFIX + userId;
        return redisTemplate.opsForList().range(redisKey, 0, -1)
                .collectList()
                .flatMap(redisPerms -> {
                    if (!redisPerms.isEmpty()) {
                        // Hit L2 -> Populate L1
                        permissionCache.put(userId, redisPerms);
                        return Mono.just(redisPerms.contains(requiredPermission));
                    }

                    // 3. Fallback to IAM Service
                    return fetchPermissionsFromIam(userId)
                            .map(iamPerms -> {
                                if (iamPerms.contains(requiredPermission))
                                    return true;
                                return false;
                            });
                });
    }

    /**
     * Fetch permissions from IAM Service and populate caches.
     */
    private Mono<List<String>> fetchPermissionsFromIam(String userId) {
        return webClient.get()
                .uri("/api/internal/permissions/user/{userId}", userId)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .doOnNext(perms -> {
                    // Populate L1
                    permissionCache.put(userId, perms);

                    // Populate L2 (Redis) - Async
                    String redisKey = PERMISSION_KEY_PREFIX + userId;
                    redisTemplate.delete(redisKey) // Clear old
                            .then(redisTemplate.opsForList().rightPushAll(redisKey, perms))
                            .then(redisTemplate.expire(redisKey, REDIS_TTL))
                            .subscribe();
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch permissions from IAM for user: {}", userId, e);
                    return Mono.just(Collections.emptyList());
                });
    }

    /**
     * Invalidate caches (called via Webhook/Event)
     */
    public Mono<Void> invalidateUserPermissions(String userId) {
        permissionCache.invalidate(userId);
        return redisTemplate.delete(PERMISSION_KEY_PREFIX + userId).then();
    }
}
