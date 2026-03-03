package com.enterprise.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.client.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Generic Health Indicator to check downstream dependencies via HTTP.
 * Can be subclassed or used as a bean for specific service checks.
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DependencyHealthIndicator implements HealthIndicator {

    private final String serviceName;
    private final String url;
    private final RestClient restClient;

    public DependencyHealthIndicator(String serviceName, String url) {
        this.serviceName = serviceName;
        this.url = url;
        this.restClient = RestClient.create();
    }

    @Override
    public Health health() {
        try {
            restClient.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();

            return Health.up()
                    .withDetail("service", serviceName)
                    .withDetail("url", url)
                    .build();
        } catch (Exception e) {
            log.warn("Health check failed for {}: {}", serviceName, e.getMessage());
            return Health.down()
                    .withDetail("service", serviceName)
                    .withDetail("url", url)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
