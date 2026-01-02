package com.enterprise.gateway.feign;

import com.enterprise.gateway.feign.error.GlobalFeignErrorDecoder;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Default Feign Client Configuration.
 * <p>
 * Provides centralized configuration for:
 * - Connection and read timeouts
 * - Retry policy
 * - Error handling
 * - Logging level
 * <p>
 * Usage:
 * <pre>
 * {@code @FeignClient(name = "my-service", configuration = FeignClientConfiguration.class)}
 * public interface MyServiceClient {
 *     // your methods
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "feign.codec.ErrorDecoder")
public class FeignClientConfiguration {

    @Value("${feign.client.config.default.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${feign.client.config.default.read-timeout:10000}")
    private int readTimeout;

    @Value("${feign.client.config.default.follow-redirects:true}")
    private boolean followRedirects;

    /**
     * Configure request options for timeout and redirect handling.
     *
     * @return Request.Options instance
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                connectTimeout, TimeUnit.MILLISECONDS,
                readTimeout, TimeUnit.MILLISECONDS,
                followRedirects
        );
    }

    /**
     * Configure retry behavior.
     * <p>
     * Default: No retry (fail-fast). Override this bean in specific clients if retry is needed.
     * For production systems, consider using Resilience4j circuit breaker instead.
     *
     * @return Retryer instance
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * Configure error decoder for unified error handling.
     *
     * @return ErrorDecoder instance
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new GlobalFeignErrorDecoder();
    }

    /**
     * Configure Feign logging level.
     * <p>
     * Options:
     * - NONE: No logging (default)
     * - BASIC: Log only request method, URL, response status, and execution time
     * - HEADERS: Log basic info + request/response headers
     * - FULL: Log headers, body, and metadata for both requests and responses
     * <p>
     * Set logging level in application.yml:
     * <pre>
     * logging:
     *   level:
     *     com.enterprise: DEBUG
     * </pre>
     *
     * @return Logger.Level instance
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
