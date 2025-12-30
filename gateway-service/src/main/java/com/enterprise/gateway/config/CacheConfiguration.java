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
     * - Maximum 10,000 entries
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "authorization",
                "userInfo",
                "tokenValidation"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()  // Enable metrics
        );

        return cacheManager;
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
