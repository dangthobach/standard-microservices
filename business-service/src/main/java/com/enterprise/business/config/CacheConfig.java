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
     * 
     * Configured with multiple cache regions:
     * - products: 10 min TTL (frequently accessed, moderate change rate)
     * - productBySku: 15 min TTL (rarely changes after creation)
     * - productStatus: 2 min TTL (changes frequently during workflow)
     * - categories: 1 hour TTL (rarely changes)
     * - productCounts: 5 min TTL (for dashboard metrics)
     */
    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Custom cache configurations with different TTLs
        java.util.Map<String, RedisCacheConfiguration> cacheConfigurations = new java.util.HashMap<>();
        
        // Products cache - 10 minutes
        cacheConfigurations.put("products", 
                defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Product by SKU - 15 minutes (rarely changes)
        cacheConfigurations.put("productBySku", 
                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Product status - 2 minutes (changes during workflow)
        cacheConfigurations.put("productStatus", 
                defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Category list - 1 hour (rarely changes)
        cacheConfigurations.put("categories", 
                defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Product counts - 5 minutes (for dashboards)
        cacheConfigurations.put("productCounts", 
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
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
