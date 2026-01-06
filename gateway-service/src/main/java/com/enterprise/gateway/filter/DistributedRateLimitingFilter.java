package com.enterprise.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.ClientOptions;
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
import java.util.concurrent.locks.ReentrantLock;
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

    private volatile ProxyManager<String> proxyManager;
    private volatile StatefulRedisConnection<String, byte[]> redisConnection;
    private volatile RedisClient redisClient;
    private final ReentrantLock connectionLock = new ReentrantLock();

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

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${ratelimit.anonymous.capacity:100}")
    private long anonymousCapacity;

    @Value("${ratelimit.authenticated.capacity:1000}")
    private long authenticatedCapacity;

    @Value("${ratelimit.premium.capacity:10000}")
    private long premiumCapacity;

    private volatile boolean redisAvailable = false;
    private volatile long lastConnectionAttempt = 0;
    private static final long CONNECTION_RETRY_INTERVAL_MS = 30_000; // Retry every 30 seconds

    @PostConstruct
    public void init() {
        // Lazy initialization - don't block startup if Redis is not ready
        // Connection will be attempted on first use with retry logic
        log.info("Distributed Rate Limiting Filter initialized. Redis connection will be established on first use.");
    }

    /**
     * Initialize Redis connection with retry logic
     * This is called lazily on first use to avoid blocking application startup
     */
    private void ensureRedisConnection() {
        // Skip if already connected
        if (redisAvailable && proxyManager != null && redisConnection != null && redisConnection.isOpen()) {
            return;
        }

        // Rate limit connection attempts (don't spam logs)
        long now = System.currentTimeMillis();
        if (now - lastConnectionAttempt < CONNECTION_RETRY_INTERVAL_MS) {
            return;
        }

        connectionLock.lock();
        try {
            // Double-check after acquiring lock
            if (redisAvailable && proxyManager != null && redisConnection != null && redisConnection.isOpen()) {
                return;
            }

            lastConnectionAttempt = now;

            // Build Redis URI with password if provided
            RedisURI.Builder uriBuilder = RedisURI.builder()
                    .withHost(redisHost)
                    .withPort(redisPort)
                    .withTimeout(Duration.ofSeconds(5));

            if (redisPassword != null && !redisPassword.isBlank()) {
                uriBuilder.withPassword(redisPassword.toCharArray());
            }

            RedisURI redisUri = uriBuilder.build();

            // Create Redis client with timeout options
            redisClient = RedisClient.create(redisUri);
            redisClient.setOptions(ClientOptions.builder()
                    .timeoutOptions(TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(5))
                            .build())
                    .build());

            // Attempt connection with timeout
            redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

            // Verify connection by sending a PING
            try {
                redisConnection.sync().ping();
            } catch (Exception e) {
                log.warn("Redis connection established but PING failed: {}", e.getMessage());
                closeConnection();
                throw e;
            }

            // Create Bucket4j ProxyManager with Redis backend
            proxyManager = LettuceBasedProxyManager.builderFor(redisConnection)
                    .build();

            log.info("✅ Distributed Rate Limiting connected to Redis backend: {}:{}",
                    redisHost, redisPort);
            redisAvailable = true;

        } catch (Exception e) {
            log.warn("❌ Failed to connect to Redis for rate limiting (will retry on next request). Falling back to local cache: {}",
                    e.getMessage());
            closeConnection();
            redisAvailable = false;
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * Safely close Redis connection
     */
    private void closeConnection() {
        try {
            if (redisConnection != null && redisConnection.isOpen()) {
                redisConnection.close();
            }
        } catch (Exception e) {
            log.debug("Error closing Redis connection: {}", e.getMessage());
        }
        redisConnection = null;
        proxyManager = null;
        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception e) {
                log.debug("Error shutting down Redis client: {}", e.getMessage());
            }
            redisClient = null;
        }
    }

    @PreDestroy
    public void cleanup() {
        closeConnection();
        log.info("Redis connection closed for rate limiting");

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
     * 1. Try Redis (distributed across all pods) - lazy connection with retry
     * 2. Fallback to local Caffeine cache if Redis down
     * 3. Caffeine cache auto-evicts after 5 min (LRU)
     */
    private Bucket resolveBucket(String identifier, String userTier) {
        // Attempt to connect to Redis if not already connected
        if (!redisAvailable || proxyManager == null) {
            ensureRedisConnection();
        }

        // Try Redis if available
        if (redisAvailable && proxyManager != null) {
            try {
                // Verify connection is still open
                if (redisConnection != null && redisConnection.isOpen()) {
                    // Use distributed Redis bucket
                    return proxyManager.builder()
                            .build(identifier, getBucketConfiguration(userTier));
                } else {
                    // Connection lost, mark as unavailable
                    redisAvailable = false;
                }
            } catch (Exception e) {
                log.debug("Redis unavailable for user: {}, falling back to local cache. Error: {}",
                        identifier, e.getMessage());
                // Mark as unavailable and close connection
                redisAvailable = false;
                closeConnection();
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

            Bandwidth limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(capacity, Duration.ofMinutes(1))
                    .build();

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

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();

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
        String ip = "unknown";
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            ip = remoteAddress.getAddress().getHostAddress();
        }

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
