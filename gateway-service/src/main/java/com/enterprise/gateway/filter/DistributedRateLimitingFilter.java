package com.enterprise.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed Rate Limiting Filter using Bucket4j with Redis Backend
 *
 * CRITICAL FIXES:
 * 1. ✅ Uses Redis as distributed backend (shared across all Gateway pods)
 * 2. ✅ Caffeine cache with TTL for local fallback (prevents memory leak)
 * 3. ✅ Automatic eviction after 5 minutes (LRU policy)
 * 4. ✅ Graceful degradation when Redis is down
 *
 * Architecture:
 * ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
 * │  Gateway 1  │───▶│    Redis    │◀───│  Gateway 2  │
 * │  (Pod 1)    │    │  (Shared)   │    │  (Pod 2)    │
 * └─────────────┘    └─────────────┘    └─────────────┘
 *       │                                      │
 *       └──────── All pods share state ───────┘
 *
 * Rate limits:
 * - Anonymous: 100 req/min
 * - Authenticated: 1000 req/min
 * - Premium: 10000 req/min
 */
@Slf4j
@Component
public class DistributedRateLimitingFilter implements GlobalFilter, Ordered {

    private ProxyManager<String> proxyManager;
    private StatefulRedisConnection<String, byte[]> redisConnection;

    /**
     * Local Caffeine cache as fallback when Redis is down
     * Maximum 50,000 entries, expire after 5 minutes
     * Prevents memory leak for 1M+ users
     */
    private final Cache<String, Bucket> localCache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${ratelimit.anonymous.capacity:100}")
    private long anonymousCapacity;

    @Value("${ratelimit.authenticated.capacity:1000}")
    private long authenticatedCapacity;

    @Value("${ratelimit.premium.capacity:10000}")
    private long premiumCapacity;

    private boolean redisAvailable = true;

    @PostConstruct
    public void init() {
        try {
            // Initialize Redis connection for Bucket4j
            RedisClient redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
            redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

            // Create Bucket4j ProxyManager with Redis backend
            proxyManager = LettuceBasedProxyManager.builderFor(redisConnection)
                    .withExpirationStrategy(
                            io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5))
                    )
                    .build();

            log.info("✅ Distributed Rate Limiting initialized with Redis backend: {}:{}",
                    redisHost, redisPort);
            redisAvailable = true;

        } catch (Exception e) {
            log.error("❌ Failed to connect to Redis for rate limiting. Falling back to local cache: {}",
                    e.getMessage());
            redisAvailable = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (redisConnection != null && redisConnection.isOpen()) {
            redisConnection.close();
            log.info("Redis connection closed for rate limiting");
        }

        // Log cache statistics
        var stats = localCache.stats();
        log.info("Local cache stats - Hits: {}, Misses: {}, Evictions: {}",
                stats.hitCount(), stats.missCount(), stats.evictionCount());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract user identifier (IP or userId)
        String identifier = getUserIdentifier(exchange);

        // Get user tier for rate limit
        String userTier = getUserTier(exchange);

        // Get or create bucket for this user
        Bucket bucket = resolveBucket(identifier, userTier);

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            // Request allowed - add rate limit headers
            addRateLimitHeaders(exchange, bucket, userTier);
            return chain.filter(exchange);

        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for user: {} (tier: {})", identifier, userTier);

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Retry-After", "60");
            addRateLimitHeaders(exchange, bucket, userTier);

            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Get or create bucket for user
     *
     * Strategy:
     * 1. Try Redis (distributed across all pods)
     * 2. Fallback to local Caffeine cache if Redis down
     * 3. Caffeine cache auto-evicts after 5 min (LRU)
     */
    private Bucket resolveBucket(String identifier, String userTier) {
        if (redisAvailable && proxyManager != null) {
            try {
                // Use distributed Redis bucket
                return proxyManager.builder()
                        .build(identifier, getBucketConfiguration(userTier));

            } catch (Exception e) {
                log.warn("Redis unavailable, falling back to local cache for user: {}. Error: {}",
                        identifier, e.getMessage());
                redisAvailable = false;
            }
        }

        // Fallback to local Caffeine cache
        return localCache.get(identifier,
                key -> createLocalBucket(userTier));
    }

    /**
     * Create bucket configuration based on user tier
     */
    private Supplier<BucketConfiguration> getBucketConfiguration(String userTier) {
        return () -> {
            long capacity = getCapacityForTier(userTier);

            Bandwidth limit = Bandwidth.classic(
                    capacity,
                    Refill.intervally(capacity, Duration.ofMinutes(1))
            );

            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }

    /**
     * Create local bucket (when Redis is down)
     */
    private Bucket createLocalBucket(String userTier) {
        long capacity = getCapacityForTier(userTier);

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Get user identifier from request
     * Priority: userId from JWT > IP address
     */
    private String getUserIdentifier(ServerWebExchange exchange) {
        // Try to get userId from JWT token (set by authentication filter)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }

        // Fallback to IP address
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        return "ip:" + ip;
    }

    /**
     * Determine user tier from JWT claims or headers
     */
    private String getUserTier(ServerWebExchange exchange) {
        // Check for premium tier header (set by authentication service)
        String tier = exchange.getRequest().getHeaders().getFirst("X-User-Tier");
        if (tier != null) {
            return tier.toUpperCase();
        }

        // Check if authenticated (has userId)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "AUTHENTICATED";
        }

        return "ANONYMOUS";
    }

    /**
     * Get capacity based on user tier
     */
    private long getCapacityForTier(String tier) {
        return switch (tier) {
            case "PREMIUM" -> premiumCapacity;
            case "AUTHENTICATED" -> authenticatedCapacity;
            default -> anonymousCapacity;
        };
    }

    /**
     * Add rate limit headers to response (RFC 6585)
     */
    private void addRateLimitHeaders(ServerWebExchange exchange, Bucket bucket, String userTier) {
        long available = bucket.getAvailableTokens();
        long capacity = getCapacityForTier(userTier);

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(capacity));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(available));
        exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() / 1000 + 60));

        // Add backend indicator for debugging
        exchange.getResponse().getHeaders().add("X-RateLimit-Backend",
                redisAvailable ? "redis" : "local-cache");
    }

    @Override
    public int getOrder() {
        // Run after authentication but before routing
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
