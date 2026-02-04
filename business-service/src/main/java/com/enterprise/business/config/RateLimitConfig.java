package com.enterprise.business.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration using Resilience4j
 * Protects APIs from abuse and ensures fair resource allocation
 */
@Configuration
public class RateLimitConfig {

    /**
     * Rate limiter for product creation
     * Limit: 100 requests per minute per user
     */
    @Bean
    public RateLimiter productCreationLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)                          // 100 requests
                .limitRefreshPeriod(Duration.ofMinutes(1))    // per minute
                .timeoutDuration(Duration.ofSeconds(5))       // wait max 5s for permission
                .build();
        
        return RateLimiter.of("productCreation", config);
    }

    /**
     * Rate limiter for product queries
     * Limit: 200 requests per minute per user
     */
    @Bean
    public RateLimiter productQueryLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(200)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        return RateLimiter.of("productQuery", config);
    }

    /**
     * Rate limiter for workflow operations
     * Limit: 50 requests per minute per user
     */
    @Bean
    public RateLimiter workflowOperationLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        return RateLimiter.of("workflowOperation", config);
    }

    /**
     * Global rate limiter registry
     * Can be used to create dynamic rate limiters
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        return RateLimiterRegistry.of(defaultConfig);
    }
}
