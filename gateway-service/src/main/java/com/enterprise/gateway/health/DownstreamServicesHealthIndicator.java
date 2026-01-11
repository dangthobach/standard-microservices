package com.enterprise.gateway.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Advanced Health Check for Downstream Services
 * <p>
 * Checks:
 * 1. Service availability (HTTP health endpoint)
 * 2. Circuit breaker state
 * 3. Response time
 * 4. Failure rate
 * <p>
 * Aggregates health of all downstream services.
 * <p>
 * IMPORTANT: Uses standard (non-load-balanced) WebClient.Builder to make
 * direct HTTP calls to configured service URLs. This bypasses service discovery
 * and prevents "No servers available for service: IP" errors.
 */
@Slf4j
@Component
public class DownstreamServicesHealthIndicator implements ReactiveHealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Standard WebClient.Builder (not @LoadBalanced) for direct HTTP calls.
     * This prevents LoadBalancer from trying to resolve IP addresses as service
     * names.
     */
    private final WebClient.Builder standardWebClientBuilder;

    // Health check URLs are now configured via application.yml
    @Value("${gateway.health.downstream-services.iam-service}")
    private String iamServiceHealthUrl;

    @Value("${gateway.health.downstream-services.business-service}")
    private String businessServiceHealthUrl;

    // Only check services that actually run in the current deployment
    private static final String[] SERVICES = {
            "iam-service",
            "business-service"
    };

    /**
     * Constructor with explicit @Qualifier for standardWebClientBuilder.
     * <p>
     * NOTE: We cannot use Lombok @RequiredArgsConstructor here because
     * 
     * @Qualifier on fields is not propagated to constructor parameters by Lombok.
     *            Spring needs @Qualifier on the constructor parameter to properly
     *            inject
     *            the correct bean when multiple WebClient.Builder beans exist.
     *
     * @param circuitBreakerRegistry   Registry for circuit breakers
     * @param standardWebClientBuilder Standard WebClient.Builder (not LoadBalanced)
     */
    public DownstreamServicesHealthIndicator(
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("standardWebClientBuilder") WebClient.Builder standardWebClientBuilder) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.standardWebClientBuilder = standardWebClientBuilder;
        log.info("✅ DownstreamServicesHealthIndicator initialized with standardWebClientBuilder (non-LoadBalanced)");
    }

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
     * Check health of all downstream services in parallel (non-blocking).
     *
     * Uses reactive WebClient + Flux to fan out requests and aggregate results.
     * - No blocking calls (no .block())
     * - Each service is checked independently
     * - Errors per service are handled in {@link #checkServiceHealth(String)}
     */
    private Mono<Map<String, Map<String, Object>>> checkAllServices() {
        return Flux.fromArray(SERVICES)
                .flatMap(service -> checkServiceHealth(service)
                        .map(health -> Map.entry(service, health)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Check health of a single service
     * <p>
     * Uses standard WebClient (not load-balanced) to make direct HTTP calls
     * to the configured service URL. This bypasses service discovery and prevents
     * LoadBalancer from trying to resolve IP addresses as service names.
     */
    private Mono<Map<String, Object>> checkServiceHealth(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        String url = getServiceHealthUrl(serviceName);

        if (url == null || url.isBlank()) {
            log.warn("No health check URL configured for service: {}", serviceName);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UNKNOWN");
            health.put("error", "No health check URL configured");
            return Mono.just(health);
        }

        long startTime = System.currentTimeMillis();

        // Use standard WebClient (not load-balanced) for direct HTTP calls
        return standardWebClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(HashMap.class)
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

    /**
     * Get health check URL for a service from configuration
     */
    private String getServiceHealthUrl(String serviceName) {
        return switch (serviceName) {
            case "iam-service" -> iamServiceHealthUrl;
            case "business-service" -> businessServiceHealthUrl;
            default -> null;
        };
    }
}
