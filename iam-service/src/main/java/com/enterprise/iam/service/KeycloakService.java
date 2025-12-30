package com.enterprise.iam.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Keycloak Service with Resilience4j Patterns
 *
 * Demonstrates usage of:
 * - @CircuitBreaker: Fail fast when Keycloak is down
 * - @Retry: Retry transient failures
 * - @RateLimiter: Throttle calls to Keycloak
 * - @Bulkhead: Isolate Keycloak calls
 *
 * Pattern Stack (applied in order):
 * 1. Bulkhead (isolation)
 * 2. CircuitBreaker (fail fast)
 * 3. RateLimiter (throttle)
 * 4. Retry (retry failures)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final RestTemplate restTemplate;

    /**
     * Get user from Keycloak with full resilience protection
     *
     * Stack:
     * - Bulkhead: Max 25 concurrent calls
     * - CircuitBreaker: Open after 50% failures
     * - RateLimiter: Max 100 calls/sec
     * - Retry: 3 attempts with exponential backoff
     */
    @Bulkhead(name = "keycloak", fallbackMethod = "getUserFallback")
    @CircuitBreaker(name = "keycloak", fallbackMethod = "getUserFallback")
    @RateLimiter(name = "keycloak")
    @Retry(name = "keycloak")
    public String getUserFromKeycloak(String userId) {
        log.debug("Fetching user {} from Keycloak", userId);

        String url = "http://keycloak:8080/admin/realms/enterprise/users/" + userId;

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Failed to fetch user from Keycloak: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method when Keycloak is unavailable
     *
     * Returns cached data or default response
     */
    public String getUserFallback(String userId, Exception ex) {
        log.warn("Keycloak unavailable, using fallback for user: {}, error: {}",
                userId, ex.getMessage());

        // Return cached user or default response
        return """
            {
              "id": "%s",
              "username": "cached_user",
              "email": "cached@example.com",
              "enabled": true,
              "source": "fallback"
            }
            """.formatted(userId);
    }

    /**
     * Validate token with Circuit Breaker only
     * (No retry - token validation should be fast or fail)
     */
    @CircuitBreaker(name = "keycloak", fallbackMethod = "validateTokenFallback")
    public boolean validateToken(String token) {
        log.debug("Validating token with Keycloak");

        String url = "http://keycloak:8080/realms/enterprise/protocol/openid-connect/userinfo";

        try {
            var response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback for token validation
     * In production, this should check local cache or deny access
     */
    public boolean validateTokenFallback(String token, Exception ex) {
        log.warn("Token validation fallback triggered: {}", ex.getMessage());

        // For safety, deny access when Keycloak is down
        // In production, you might check a local cache first
        return false;
    }

    /**
     * Sync users from Keycloak
     * Uses Bulkhead to prevent overwhelming the system
     */
    @Bulkhead(name = "keycloak-sync", type = Bulkhead.Type.THREADPOOL)
    @Retry(name = "keycloak-sync")
    public void syncUsers() {
        log.info("Starting user sync from Keycloak");

        // This is a long-running operation
        // Bulkhead with THREADPOOL ensures it runs in isolation
        // and doesn't block other operations

        try {
            String url = "http://keycloak:8080/admin/realms/enterprise/users";
            var users = restTemplate.getForObject(url, String.class);

            log.info("Successfully synced users from Keycloak");
        } catch (Exception e) {
            log.error("User sync failed: {}", e.getMessage());
            throw e;
        }
    }
}
