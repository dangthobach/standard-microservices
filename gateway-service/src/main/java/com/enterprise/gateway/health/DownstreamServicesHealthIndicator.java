package com.enterprise.gateway.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Advanced Health Check for Downstream Services
 *
 * Checks:
 * 1. Service availability (HTTP health endpoint)
 * 2. Circuit breaker state
 * 3. Response time
 * 4. Failure rate
 *
 * Aggregates health of all downstream services
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownstreamServicesHealthIndicator implements ReactiveHealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final WebClient.Builder webClientBuilder;

    private static final String[] SERVICES = {
            "iam-service",
            "business-service",
            "process-service",
            "integration-service"
    };

    private static final Map<String, String> SERVICE_URLS = Map.of(
            "iam-service", "http://iam-service:8081/actuator/health",
            "business-service", "http://business-service:8082/actuator/health",
            "process-service", "http://process-service:8083/actuator/health",
            "integration-service", "http://integration-service:8084/actuator/health"
    );

    @Override
    public Mono<Health> health() {
        return checkAllServices()
                .map(servicesHealth -> {
                    boolean allHealthy = servicesHealth.values().stream()
                            .allMatch(health -> health.get("status").equals("UP"));

                    Health.Builder builder = allHealthy ? Health.up() : Health.down();
                    servicesHealth.forEach(builder::withDetail);

                    return builder.build();
                })
                .onErrorResume(error -> {
                    log.error("Error checking downstream services health", error);
                    return Mono.just(Health.down()
                            .withException(error)
                            .build());
                });
    }

    /**
     * Check health of all downstream services in parallel
     */
    private Mono<Map<String, Map<String, Object>>> checkAllServices() {
        Map<String, Mono<Map<String, Object>>> healthChecks = new HashMap<>();

        for (String service : SERVICES) {
            healthChecks.put(service, checkServiceHealth(service));
        }

        // Execute all health checks in parallel
        return Mono.zip(
                healthChecks.values(),
                results -> {
                    Map<String, Map<String, Object>> combined = new HashMap<>();
                    int i = 0;
                    for (String service : SERVICES) {
                        combined.put(service, (Map<String, Object>) results[i++]);
                    }
                    return combined;
                }
        );
    }

    /**
     * Check health of a single service
     */
    private Mono<Map<String, Object>> checkServiceHealth(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        String url = SERVICE_URLS.get(serviceName);

        long startTime = System.currentTimeMillis();

        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(2))
                .map(response -> {
                    long responseTime = System.currentTimeMillis() - startTime;

                    Map<String, Object> health = new HashMap<>();
                    health.put("status", "UP");
                    health.put("responseTime", responseTime + "ms");
                    health.put("circuitBreakerState", circuitBreaker.getState().name());
                    health.put("failureRate", String.format("%.2f%%",
                            circuitBreaker.getMetrics().getFailureRate()));
                    health.put("slowCallRate", String.format("%.2f%%",
                            circuitBreaker.getMetrics().getSlowCallRate()));

                    return health;
                })
                .onErrorResume(error -> {
                    log.warn("Service {} is DOWN: {}", serviceName, error.getMessage());

                    Map<String, Object> health = new HashMap<>();
                    health.put("status", "DOWN");
                    health.put("error", error.getMessage());
                    health.put("circuitBreakerState", circuitBreaker.getState().name());

                    return Mono.just(health);
                });
    }
}
