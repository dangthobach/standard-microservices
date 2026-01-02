package com.enterprise.iam.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Multi-Level Cache Configuration for IAM Service
 * <p>
 * Implements L1 (Caffeine in-memory) + L2 (Redis distributed) caching strategy.
 * <p>
 * L1 Cache (Caffeine):
 * - Ultra-fast in-memory cache within the application
 * - Reduces network calls to Redis for frequently accessed data
 * - Critical for 1M CCU performance (roles, permissions lookups)
 * - Auto-eviction prevents memory leaks
 * <p>
 * L2 Cache (Redis):
 * - Distributed cache shared across service instances
 * - Configured via application.yml (spring.cache.type=redis)
 * <p>
 * Cache Strategy:
 * - User roles/permissions: L1 + L2 (read-heavy, rarely changes)
 * - Keycloak tokens: L2 only (needs to be shared)
 * - Session data: L2 only (distributed sessions)
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    /**
     * Caffeine L1 Cache Manager
     * <p>
     * Configured for high-concurrency scenarios:
     * - Maximum size: 10,000 entries per cache
     * - TTL: 5 minutes (roles/permissions don't change often)
     * - Eviction: LRU (Least Recently Used)
     * - Record stats: Monitor hit/miss ratio
     *
     * @return Caffeine cache manager
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "users",           // User cache by ID
            "usersByEmail",    // User cache by email
            "usersByKeycloakId", // User cache by Keycloak ID
            "roles",           // Role cache
            "permissions",     // Permission cache
            "userRoles"        // User-Role mappings cache
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)  // Max 10k entries per cache
            .expireAfterWrite(5, TimeUnit.MINUTES)  // TTL: 5 minutes
            .expireAfterAccess(3, TimeUnit.MINUTES) // Evict if not accessed for 3 minutes
            .recordStats()  // Enable metrics for monitoring
        );

        return cacheManager;
    }
}
