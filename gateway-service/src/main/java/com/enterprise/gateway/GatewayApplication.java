package com.enterprise.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API Gateway Application - Reactive WebFlux Gateway
 * - Handles all incoming requests
 * - Performs OAuth2 PKCE authentication with Keycloak
 * - Implements L1 Caffeine cache for authorization decisions
 * - Initiates distributed tracing (TraceId)
 * - Routes to downstream microservices
 * - Collects CCU (Concurrent Users) metrics via scheduled tasks
 */
@SpringBootApplication
@ComponentScan(
    basePackages = {"com.enterprise.gateway", "com.enterprise.common"},
    // ⚠️ CRITICAL: Exclude Feign package - Gateway is Reactive and incompatible with blocking Feign
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.enterprise\\.common\\.feign\\..*"
    )
)
@EnableCaching
@EnableScheduling
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
