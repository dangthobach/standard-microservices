package com.enterprise.gateway.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Resilience Service - Helper for applying resilience patterns
 *
 * Provides easy-to-use methods to wrap reactive calls with:
 * - Circuit Breaker
 * - Retry
 * - Rate Limiter
 * - Bulkhead
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilienceService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    /**
     * Apply all resilience patterns to a Mono
     *
     * Order of execution:
     * 1. Bulkhead (request coalescing & isolation)
     * 2. Circuit Breaker (fail fast if service unhealthy)
     * 3. Rate Limiter (throttle requests)
     * 4. Retry (automatic retry on failure)
     *
     * @param serviceName Name of downstream service
     * @param supplier Mono supplier
     * @param fallback Fallback function on error
     * @return Protected Mono
     */
    public <T> Mono<T> executeWithResilience(
            String serviceName,
            Mono<T> supplier,
            Function<Throwable, Mono<T>> fallback) {

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(serviceName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(serviceName);
        Retry retry = retryRegistry.retry(serviceName);

        return supplier
                // 1. Bulkhead - Isolate & coalesce requests
                .transformDeferred(BulkheadOperator.of(bulkhead))

                // 2. Circuit Breaker - Fail fast if service unhealthy
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))

                // 3. Rate Limiter - Throttle requests
                .transformDeferred(RateLimiterOperator.of(rateLimiter))

                // 4. Retry - Automatic retry on transient failures
                .transformDeferred(RetryOperator.of(retry))

                // 5. Fallback - Handle final failures gracefully
                .onErrorResume(throwable -> {
                    log.error("Service [{}] failed after all resilience attempts: {}",
                            serviceName, throwable.getMessage());
                    return fallback.apply(throwable);
                });
    }

    /**
     * Apply resilience patterns without fallback
     * Will propagate error if all retries fail
     */
    public <T> Mono<T> executeWithResilience(String serviceName, Mono<T> supplier) {
        return executeWithResilience(serviceName, supplier, Mono::error);
    }

    /**
     * Apply only Circuit Breaker
     */
    public <T> Mono<T> executeWithCircuitBreaker(String serviceName, Mono<T> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        return supplier.transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    /**
     * Apply only Retry
     */
    public <T> Mono<T> executeWithRetry(String serviceName, Mono<T> supplier) {
        Retry retry = retryRegistry.retry(serviceName);
        return supplier.transformDeferred(RetryOperator.of(retry));
    }

    /**
     * Apply only Rate Limiter
     */
    public <T> Mono<T> executeWithRateLimiter(String serviceName, Mono<T> supplier) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(serviceName);
        return supplier.transformDeferred(RateLimiterOperator.of(rateLimiter));
    }

    /**
     * Apply only Bulkhead
     */
    public <T> Mono<T> executeWithBulkhead(String serviceName, Mono<T> supplier) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(serviceName);
        return supplier.transformDeferred(BulkheadOperator.of(bulkhead));
    }

    /**
     * Get circuit breaker state
     */
    public String getCircuitBreakerState(String serviceName) {
        return circuitBreakerRegistry.circuitBreaker(serviceName)
                .getState()
                .name();
    }

    /**
     * Get circuit breaker metrics
     */
    public CircuitBreaker.Metrics getCircuitBreakerMetrics(String serviceName) {
        return circuitBreakerRegistry.circuitBreaker(serviceName)
                .getMetrics();
    }
}
