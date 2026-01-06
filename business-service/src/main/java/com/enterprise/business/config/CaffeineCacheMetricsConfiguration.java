package com.enterprise.business.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

/**
 * Caffeine Cache Metrics Configuration
 * <p>
 * Exposes Prometheus metrics for monitoring Caffeine L1 cache performance:
 * - cache.size: Current number of entries in the cache
 * - cache.hit_count: Total number of cache hits
 * - cache.miss_count: Total number of cache misses
 * - cache.hit_rate: Cache hit rate (hits / (hits + misses))
 * - cache.eviction_count: Total number of evictions
 * - cache.load_count: Total number of loads (cache misses that triggered loads)
 * - cache.load_time: Total time spent loading new values (nanoseconds)
 * <p>
 * Metrics are tagged by cache name (e.g., "products") for multi-cache scenarios.
 * <p>
 * Access metrics via:
 * - /actuator/metrics/cache.size?tag=cache:products
 * - /actuator/metrics/cache.hit_rate?tag=cache:products
 * - /actuator/prometheus (for Prometheus scraping)
 * <p>
 * Example Prometheus queries:
 * - Cache hit rate: cache_hit_rate{cache="products"}
 * - Total hits: cache_hit_count_total{cache="products"}
 * - Cache size: cache_size{cache="products"}
 * - Hit rate percentage: cache_hit_rate{cache="products"} * 100
 * <p>
 * Performance targets:
 * - L1 hit rate target: > 90% for frequently accessed products
 * - L1 hit latency: ~1µs (vs Redis ~1ms = 1000x faster)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CaffeineCacheMetricsConfiguration {

    private final MeterRegistry meterRegistry;
    
    @Qualifier("caffeineCacheManager")
    private final CaffeineCacheManager caffeineCacheManager;

    @PostConstruct
    public void registerCacheMetrics() {
        Collection<String> cacheNames = caffeineCacheManager.getCacheNames();
        
        if (cacheNames.isEmpty()) {
            log.warn("No Caffeine caches found, skipping metrics registration");
            return;
        }

        for (String cacheName : cacheNames) {
            org.springframework.cache.Cache cache = caffeineCacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                Cache<?, ?> nativeCache = caffeineCache.getNativeCache();
                
                if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                    registerMetricsForCache(cacheName, (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache);
                }
            }
        }

        log.info("✅ Registered Caffeine cache metrics for {} cache(s)", cacheNames.size());
    }

    private void registerMetricsForCache(String cacheName, com.github.benmanes.caffeine.cache.Cache<?, ?> cache) {
        List<Tag> tags = List.of(Tag.of("cache", cacheName));

        // Cache size (current number of entries)
        Gauge.builder("cache.size", cache, c -> (double) c.estimatedSize())
            .description("Current number of entries in the cache")
            .baseUnit("entries")
            .tags(tags)
            .register(meterRegistry);

        // Cache stats (requires stats to be enabled via recordStats())
        Gauge.builder("cache.hit_count", cache, c -> {
            CacheStats stats = c.stats();
            return (double) stats.hitCount();
        })
        .description("Total number of cache hits")
        .baseUnit("hits")
        .tags(tags)
        .register(meterRegistry);

        Gauge.builder("cache.miss_count", cache, c -> {
            CacheStats stats = c.stats();
            return (double) stats.missCount();
        })
        .description("Total number of cache misses")
        .baseUnit("misses")
        .tags(tags)
        .register(meterRegistry);

        // Cache hit rate (hits / (hits + misses))
        Gauge.builder("cache.hit_rate", cache, c -> {
            CacheStats stats = c.stats();
            long hits = stats.hitCount();
            long misses = stats.missCount();
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        })
        .description("Cache hit rate (0.0 to 1.0)")
        .baseUnit("ratio")
        .tags(tags)
        .register(meterRegistry);

        // Eviction count
        Gauge.builder("cache.eviction_count", cache, c -> {
            CacheStats stats = c.stats();
            return (double) stats.evictionCount();
        })
        .description("Total number of cache evictions")
        .baseUnit("evictions")
        .tags(tags)
        .register(meterRegistry);

        // Load count (cache misses that triggered loads)
        Gauge.builder("cache.load_count", cache, c -> {
            CacheStats stats = c.stats();
            return (double) stats.loadCount();
        })
        .description("Total number of cache loads")
        .baseUnit("loads")
        .tags(tags)
        .register(meterRegistry);

        // Load time (total nanoseconds spent loading)
        Gauge.builder("cache.load_time", cache, c -> {
            CacheStats stats = c.stats();
            return (double) stats.totalLoadTime();
        })
        .description("Total time spent loading new values")
        .baseUnit("nanoseconds")
        .tags(tags)
        .register(meterRegistry);

        log.debug("Registered metrics for cache: {}", cacheName);
    }
}

