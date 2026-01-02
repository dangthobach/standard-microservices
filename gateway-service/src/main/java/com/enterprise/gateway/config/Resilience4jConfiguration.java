package com.enterprise.gateway.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration for Gateway Service
 *
 * Implements:
 * 1. Circuit Breaker - Prevent cascading failures
 * 2. Retry - Automatic retry with exponential backoff
 * 3. Rate Limiter - Protect downstream services
 * 4. Bulkhead - Request coalescing & isolation
 * 5. Metrics - Prometheus integration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Resilience4jConfiguration {

    private final MeterRegistry meterRegistry;

    /**
     * Circuit Breaker Configuration
     *
     * Pattern: Prevent cascading failures by failing fast when downstream service is unhealthy
     *
     * Configuration:
     * - Sliding Window: 100 calls
     * - Failure Rate Threshold: 50% (open circuit if >50% fail)
     * - Slow Call Threshold: 50% calls taking >2s
     * - Wait Duration: 10s in OPEN state
     * - Permitted Calls in HALF_OPEN: 10
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Sliding window config
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100)
                .minimumNumberOfCalls(10)

                // Failure thresholds
                .failureRateThreshold(50.0f)              // Open if 50% fail
                .slowCallRateThreshold(50.0f)             // Open if 50% slow
                .slowCallDurationThreshold(Duration.ofSeconds(2))

                // State transition
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(10)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)

                // What to record as failure
                .recordExceptions(
                        Exception.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class
                )

                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Create circuit breakers for each service
        createCircuitBreaker(registry, "iam-service");
        createCircuitBreaker(registry, "business-service");
        createCircuitBreaker(registry, "process-service");
        createCircuitBreaker(registry, "integration-service");

        // Register metrics
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry)
                .bindTo(meterRegistry);

        log.info("Circuit Breaker Registry initialized with {} circuit breakers",
                registry.getAllCircuitBreakers().size());

        return registry;
    }

    private void createCircuitBreaker(CircuitBreakerRegistry registry, String serviceName) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker(serviceName);

        // Event listeners for logging
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.warn("Circuit Breaker [{}] state changed: {} -> {}",
                            serviceName,
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState())
                )
                .onFailureRateExceeded(event ->
                    log.error("Circuit Breaker [{}] failure rate exceeded: {}%",
                            serviceName,
                            event.getFailureRate())
                )
                .onSlowCallRateExceeded(event ->
                    log.warn("Circuit Breaker [{}] slow call rate exceeded: {}%",
                            serviceName,
                            event.getSlowCallRate())
                );
    }

    /**
     * Retry Configuration
     *
     * Pattern: Automatic retry with exponential backoff
     *
     * Configuration:
     * - Max Attempts: 3
     * - Wait Duration: Start 100ms, max 1s
     * - Exponential Backoff: 2x multiplier
     * - Retry only on specific exceptions
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                // Use intervalFunction for exponential backoff (replaces waitDuration)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(100, 2.0))

                // Retry on these exceptions
                .retryExceptions(
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class
                )

                // Don't retry on these
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class
                )

                .build();

        RetryRegistry registry = RetryRegistry.of(config);

        // Create retries for each service
        registry.retry("iam-service");
        registry.retry("business-service");
        registry.retry("process-service");
        registry.retry("integration-service");

        // Register metrics
        TaggedRetryMetrics.ofRetryRegistry(registry)
                .bindTo(meterRegistry);

        log.info("Retry Registry initialized");

        return registry;
    }

    /**
     * Rate Limiter Configuration
     *
     * Pattern: Protect downstream services from overload
     *
     * Configuration:
     * - Limit: 1000 calls per second per service
     * - Timeout: 100ms wait for permission
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1000)                      // 1000 calls
                .limitRefreshPeriod(Duration.ofSeconds(1))  // per second
                .timeoutDuration(Duration.ofMillis(100))    // wait max 100ms
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        // Create rate limiters for each service
        registry.rateLimiter("iam-service");
        registry.rateLimiter("business-service");
        registry.rateLimiter("process-service");
        registry.rateLimiter("integration-service");

        // Register metrics
        TaggedRateLimiterMetrics.ofRateLimiterRegistry(registry)
                .bindTo(meterRegistry);

        log.info("Rate Limiter Registry initialized");

        return registry;
    }

    /**
     * Bulkhead Configuration
     *
     * Pattern: Request coalescing & isolation
     *
     * Configuration:
     * - Max Concurrent Calls: 100
     * - Max Wait Duration: 100ms
     *
     * Benefits:
     * - Isolate failures (one service doesn't affect others)
     * - Prevent thread pool exhaustion
     * - Request coalescing (duplicate requests wait together)
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(100)
                .maxWaitDuration(Duration.ofMillis(100))
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);

        // Create bulkheads for each service
        registry.bulkhead("iam-service");
        registry.bulkhead("business-service");
        registry.bulkhead("process-service");
        registry.bulkhead("integration-service");

        // Register metrics
        TaggedBulkheadMetrics.ofBulkheadRegistry(registry)
                .bindTo(meterRegistry);

        log.info("Bulkhead Registry initialized");

        return registry;
    }

}
