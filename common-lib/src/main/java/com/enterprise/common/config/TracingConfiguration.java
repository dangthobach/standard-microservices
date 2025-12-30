package com.enterprise.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Distributed Tracing Configuration
 *
 * Spring Boot 3.x provides built-in tracing support via Micrometer Tracing.
 * Configuration is done via application.yml:
 *
 * management:
 *   tracing:
 *     sampling:
 *       probability: 1.0
 *   zipkin:
 *     tracing:
 *       endpoint: http://localhost:9411/api/v2/spans
 *
 * Dependencies are already included:
 * - micrometer-tracing-bridge-brave
 * - zipkin-reporter-brave
 *
 * TraceID and SpanID are automatically propagated via B3 headers.
 */
@Configuration
public class TracingConfiguration {
    // Spring Boot auto-configuration handles all tracing setup
    // No manual bean configuration required
}
