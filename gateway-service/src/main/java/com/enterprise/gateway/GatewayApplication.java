package com.enterprise.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * API Gateway Application - Reactive WebFlux Gateway
 * - Handles all incoming requests
 * - Performs OAuth2 PKCE authentication with Keycloak
 * - Implements L1 Caffeine cache for authorization decisions
 * - Initiates distributed tracing (TraceId)
 * - Routes to downstream microservices
 */
@SpringBootApplication(scanBasePackages = {"com.enterprise.gateway", "com.enterprise.common"})
@EnableCaching
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
