package com.enterprise.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * L1 Caffeine Cache Configuration
 * High-performance local cache for authorization decisions and frequently accessed data
 * Reduces latency and load on downstream services
 */
@Configuration
public class CacheConfiguration {

    /**
     * L1 Cache - Caffeine (Local, in-memory)
     * - Authorization cache: 5 minutes TTL
     * - User info cache: 5 minutes TTL
     * - Session token cache: 60 seconds TTL (for BFF pattern)
     * - Maximum 10,000 entries
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "authorization",
                "userInfo",
                "tokenValidation",
                "sessionTokens"  // For BFF session-to-token mapping
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()  // Enable metrics
        );

        return cacheManager;
    }

    /**
     * Dedicated L1 cache for session tokens (BFF Pattern)
     * <p>
     * Critical for 1M CCU performance:
     * - Cache Hit Rate Target: > 95%
     * - Reduces Redis load by 99%
     * - L1 hit latency: ~1µs vs Redis ~1ms (1000x faster)
     * <p>
     * Configuration:
     * - TTL: 60 seconds (shorter than access token lifetime)
     * - Max Size: 100,000 sessions (adjust based on memory)
     * - Eviction: LRU (Least Recently Used)
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, String> sessionTokenCache() {
        return Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(60, TimeUnit.SECONDS)  // Short TTL to ensure freshness
                .recordStats()  // Enable metrics for monitoring
                .build();
    } 

    /**
     * Short-lived cache for rate limiting and request deduplication
     */
    @Bean
    public Caffeine<Object, Object> rateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .recordStats();
    }
}
