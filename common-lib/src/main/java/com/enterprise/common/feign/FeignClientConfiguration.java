package com.enterprise.common.feign;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Base Feign Client Configuration
 * <p>
 * Provides common configuration for all Feign clients:
 * - Connection and read timeouts
 * - Retry strategy
 * - Logging level (profile-based for production safety)
 * - Error decoder
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Configuration
public class FeignClientConfiguration {

    @Value("${feign.client.config.default.logger-level:BASIC}")
    private String loggerLevel;

    /**
     * Configure request options with timeouts.
     * <p>
     * Connection timeout: 5 seconds
     * Read timeout: 10 seconds
     *
     * @return Request options
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000,   // Connect timeout (ms)
            10000   // Read timeout (ms)
        );
    }

    /**
     * Configure retry strategy.
     * <p>
     * Retries up to 3 times with exponential backoff:
     * - Initial interval: 100ms
     * - Max interval: 1000ms
     * - Multiplier: 1.5
     *
     * @return Retryer configuration
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100,   // Initial interval (ms)
            1000,  // Max interval (ms)
            3      // Max attempts
        );
    }

    /**
     * Configure logging level for Feign clients.
     * <p>
     * Logging levels:
     * - NONE: No logging (production recommended for 1M CCU)
     * - BASIC: Request method, URL, response status (minimal overhead)
     * - HEADERS: BASIC + request/response headers
     * - FULL: Everything (dev only - causes massive I/O overhead)
     * <p>
     * Configurable via: feign.client.config.default.logger-level
     * Default: BASIC
     *
     * @return Logger level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.valueOf(loggerLevel.toUpperCase());
    }

    /**
     * Configure custom error decoder.
     * <p>
     * Default error decoder handles standard HTTP error codes.
     * Override this bean to provide custom error handling.
     *
     * @return Error decoder
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder.Default();
    }
}
