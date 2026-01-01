package com.enterprise.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Global filter to track all requests for dashboard metrics
 * Monitors RPS, latency, error rate, and traffic history
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dashboard.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsGlobalFilter implements GlobalFilter, Ordered {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DASHBOARD_RPS_KEY = "dashboard:rps";
    private static final String DASHBOARD_LATENCY_KEY = "dashboard:latency:avg";
    private static final String DASHBOARD_ERROR_COUNT_KEY = "dashboard:error:count";
    private static final String DASHBOARD_REQUEST_COUNT_KEY = "dashboard:request:count";
    private static final String DASHBOARD_TRAFFIC_HISTORY_KEY = "dashboard:traffic:history";
    private static final String DASHBOARD_SLOW_ENDPOINT_PREFIX = "dashboard:slow:endpoint:";

    private static final int TRAFFIC_HISTORY_MAX_SIZE = 288; // 24 hours with 5-minute intervals

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> recordMetrics(exchange, startTime, false))
                .doOnError(error -> recordMetrics(exchange, startTime, true))
                .then(Mono.fromRunnable(
                        () -> recordMetrics(exchange, startTime, isErrorResponse(exchange.getResponse()))));
    }

    /**
     * Record metrics asynchronously to avoid blocking the request
     * Uses Redis Pipelining to batch counter increments (reduce from 7 calls to 2)
     */
    private void recordMetrics(ServerWebExchange exchange, long startTime, boolean isError) {
        Mono.fromRunnable(() -> {
            try {
                long latency = System.currentTimeMillis() - startTime;
                ServerHttpRequest request = exchange.getRequest();
                String method = request.getMethod().name();
                String path = request.getPath().value();

                // ✅ OPTIMIZED: Use Redis Pipeline to batch all counter increments
                // This reduces from 4-7 separate Redis calls to 1 pipelined batch
                recordCountersWithPipeline(latency, isError);

                // Update average latency (Exponential Moving Average)
                // This is kept separate as it requires read-modify-write
                updateExponentialMovingAverage(DASHBOARD_LATENCY_KEY, (double) latency, 0.2);

                // Record slow endpoints (>500ms) - less frequent, kept separate
                if (latency > 500) {
                    recordSlowEndpoint(method, path, latency);
                }

            } catch (Exception e) {
                log.error("Failed to record metrics: {}", e.getMessage());
            }
        })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    /**
     * Record all counter increments in a single Redis pipeline
     * Reduces network round-trips from 4-7 to 1
     */
    private void recordCountersWithPipeline(long latency, boolean isError) {
        try {
            // Create timestamp bucket for traffic history (5-minute intervals)
            long currentMinute = System.currentTimeMillis() / 300000; // 5-minute buckets
            String timestamp = String.valueOf(currentMinute * 300000);
            String requestKey = DASHBOARD_TRAFFIC_HISTORY_KEY + ":" + timestamp + ":requests";
            String errorKey = DASHBOARD_TRAFFIC_HISTORY_KEY + ":" + timestamp + ":errors";

            // ✅ Execute all counter increments in single pipeline
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                byte[] rpsKeyBytes = DASHBOARD_RPS_KEY.getBytes();
                byte[] requestCountKeyBytes = DASHBOARD_REQUEST_COUNT_KEY.getBytes();
                byte[] requestKeyBytes = requestKey.getBytes();
                byte[] errorCountKeyBytes = DASHBOARD_ERROR_COUNT_KEY.getBytes();
                byte[] errorKeyBytes = errorKey.getBytes();

                // Increment RPS counter
                connection.incr(rpsKeyBytes);
                connection.expire(rpsKeyBytes, 2); // 2-second TTL for sliding window

                // Increment total request count
                connection.incr(requestCountKeyBytes);

                // Increment traffic history request count
                connection.incr(requestKeyBytes);
                connection.expire(requestKeyBytes, 86400); // 24-hour TTL

                // Track errors if applicable
                if (isError) {
                    connection.incr(errorCountKeyBytes);
                    connection.incr(errorKeyBytes);
                    connection.expire(errorKeyBytes, 86400); // 24-hour TTL
                }

                return null;
            });

            log.debug("Recorded metrics with pipeline: latency={}ms, isError={}", latency, isError);

        } catch (Exception e) {
            log.error("Failed to record counters with pipeline: {}", e.getMessage());
        }
    }

    /**
     * Check if response is an error (4xx or 5xx)
     */
    private boolean isErrorResponse(ServerHttpResponse response) {
        if (response.getStatusCode() == null) {
            return false;
        }
        int statusCode = response.getStatusCode().value();
        return statusCode >= 400;
    }

    /**
     * Update exponential moving average for latency
     */
    private void updateExponentialMovingAverage(String key, double newValue, double alpha) {
        try {
            String currentValueStr = redisTemplate.opsForValue().get(key);
            double currentValue = currentValueStr != null ? Double.parseDouble(currentValueStr) : newValue;
            double ema = alpha * newValue + (1 - alpha) * currentValue;
            redisTemplate.opsForValue().set(key, String.valueOf(ema), Duration.ofMinutes(5));
        } catch (Exception e) {
            redisTemplate.opsForValue().set(key, String.valueOf(newValue), Duration.ofMinutes(5));
        }
    }

    /**
     * Record slow endpoint for performance monitoring
     */
    private void recordSlowEndpoint(String method, String path, long latency) {
        try {
            String key = DASHBOARD_SLOW_ENDPOINT_PREFIX + method + ":" + path;

            Map<String, Object> endpointData = new HashMap<>();
            endpointData.put("method", method);
            endpointData.put("path", path);

            // Update call count
            String callCountKey = key + ":calls";
            redisTemplate.opsForValue().increment(callCountKey);
            redisTemplate.expire(callCountKey, Duration.ofHours(1));

            // Update average latency
            String avgLatencyKey = key + ":avg";
            updateExponentialMovingAverage(avgLatencyKey, (double) latency, 0.3);

            // Update P95 (simple approximation - store max of recent values)
            String p95Key = key + ":p95";
            String currentP95Str = redisTemplate.opsForValue().get(p95Key);
            long currentP95 = currentP95Str != null ? Long.parseLong(currentP95Str) : latency;
            if (latency > currentP95) {
                redisTemplate.opsForValue().set(p95Key, String.valueOf(latency), Duration.ofHours(1));
            }

        } catch (Exception e) {
            log.error("Failed to record slow endpoint: {}", e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return -2; // Run before JwtEnrichmentFilter (which is -1)
    }
}
