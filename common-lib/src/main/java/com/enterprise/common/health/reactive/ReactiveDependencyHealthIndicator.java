package com.enterprise.common.health.reactive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Reactive Health Indicator to check downstream dependencies via HTTP in WebFlux applications.
 * Uses non-blocking WebClient and is only active in reactive web application contexts.
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveDependencyHealthIndicator implements ReactiveHealthIndicator {

    private final String serviceName;
    private final String url;
    private final WebClient webClient;

    public ReactiveDependencyHealthIndicator(String serviceName, String url) {
        this(serviceName, url, WebClient.builder().build());
    }

    @Override
    public Mono<Health> health() {
        return webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .map(response -> Health.up()
                        .withDetail("service", serviceName)
                        .withDetail("url", url)
                        .build())
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("Reactive health check timed out for {} after 5s", serviceName);
                    return Mono.just(Health.down()
                            .withDetail("service", serviceName)
                            .withDetail("url", url)
                            .withDetail("error", "Health check timed out after 5s")
                            .build());
                })
                .onErrorResume(e -> {
                    log.warn("Reactive health check failed for {}: {}", serviceName, e.getMessage());
                    return Mono.just(Health.down()
                            .withDetail("service", serviceName)
                            .withDetail("url", url)
                            .withDetail("error", e.getMessage())
                            .build());
                });
    }
}

