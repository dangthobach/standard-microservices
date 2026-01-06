package com.enterprise.business.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration
 * <p>
 * Configures Multi-Level Caching:
 * 1. Caffeine (Local/L1) - For ultra low latency
 * 2. Redis (Distributed/L2) - For shared state
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Primary Cache Manager (Redis L2)
     * Used by @Cacheable by default
     */
    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Local Cache Manager (Caffeine L1)
     * <p>
     * Optimized for high-concurrency scenarios:
     * - Products cache: 5 minutes TTL, 10k entries max
     * - Ultra-low latency for hot data (read-heavy workloads)
     * - Use explicitly via @Cacheable(cacheManager = "caffeineCacheManager")
     * <p>
     * Performance targets:
     * - L1 hit latency: ~1µs (vs Redis ~1ms = 1000x faster)
     * - Cache hit rate target: > 90% for frequently accessed products
     * - Reduces Redis load by ~90% for hot data
     */
    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("products");
        
        // Optimized Caffeine spec for products cache
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)                    // Max 10k product entries
                .expireAfterWrite(5, TimeUnit.MINUTES)  // TTL: 5 minutes (products don't change often)
                .expireAfterAccess(3, TimeUnit.MINUTES) // Evict if not accessed for 3 minutes
                .recordStats()                          // Enable metrics for monitoring
        );
        
        return cacheManager;
    }
}
