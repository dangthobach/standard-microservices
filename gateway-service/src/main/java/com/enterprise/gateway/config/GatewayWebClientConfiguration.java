package com.enterprise.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gateway WebClient Configuration
 * <p>
 * This configuration extends the common-lib WebClientConfiguration with
 * Gateway-specific WebClient Builders for service discovery.
 * <p>
 * Provides two types of WebClient Builders:
 * <p>
 * 1. {@code @LoadBalanced} WebClient.Builder:
 *    - For inter-service communication using service discovery (Consul/K8s)
 *    - Resolves service names like "iam-service" via service discovery
 *    - Use with URLs like: "lb://iam-service" or "http://iam-service"
 *    - Used by: PolicyManager, AuthZService, UserRoleService
 * <p>
 * 2. Standard WebClient.Builder (named "standardWebClientBuilder"):
 *    - For direct HTTP calls to specific endpoints (bypasses service discovery)
 *    - Use with full URLs like: "http://iam-service:8081/actuator/health"
 *    - Used by: DownstreamServicesHealthIndicator, TokenRefreshService
 * <p>
 * This separation prevents LoadBalancer from trying to resolve IP addresses
 * or direct URLs as service names, which causes "No servers available" errors.
 * <p>
 * Note: The common-lib provides a standard WebClient.Builder with timeouts/logging.
 * This configuration adds LoadBalanced support for the Gateway service.
 */
@Slf4j
@Configuration
public class GatewayWebClientConfiguration {

    /**
     * LoadBalanced WebClient.Builder with service discovery support.
     * <p>
     * This bean enables WebClient to resolve service names from Consul
     * and load balance across multiple service instances.
     * <p>
     * Usage:
     * <pre>
     * {@code
     * @Autowired
     * @LoadBalanced
     * private WebClient.Builder loadBalancedWebClientBuilder;
     *
     * WebClient client = loadBalancedWebClientBuilder
     *     .baseUrl("lb://iam-service")
     *     .build();
     * }
     * </pre>
     */
    @Bean
    @Primary
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(
            ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter) {
        log.info("✅ Configuring LoadBalanced WebClient.Builder with service discovery");
        if (loadBalancerFilter == null) {
            throw new IllegalStateException("ReactorLoadBalancerExchangeFilterFunction is required for LoadBalanced WebClient");
        }
        return WebClient.builder()
                .filter(loadBalancerFilter);
    }

    /**
     * Standard WebClient.Builder for direct HTTP calls (bypasses service discovery).
     * <p>
     * This bean is used for direct HTTP calls to specific endpoints,
     * such as health checks using Docker DNS or K8s service addresses.
     * <p>
     * Note: This creates a new builder without the LoadBalancer filter.
     * The common-lib's WebClientConfiguration provides timeout and logging,
     * but we need a separate builder that doesn't go through service discovery.
     * <p>
     * Usage:
     * <pre>
     * {@code
     * @Autowired
     * @Qualifier("standardWebClientBuilder")
     * private WebClient.Builder standardWebClientBuilder;
     *
     * WebClient client = standardWebClientBuilder.build();
     * client.get()
     *     .uri("http://iam-service:8081/actuator/health")
     *     .retrieve()
     *     .bodyToMono(String.class);
     * }
     * </pre>
     */
    @Bean(name = "standardWebClientBuilder")
    public WebClient.Builder standardWebClientBuilder() {
        log.info("✅ Configuring Standard WebClient.Builder for direct HTTP calls (bypasses service discovery)");
        // Create a standard builder without LoadBalancer filter
        // This allows direct HTTP calls to IP addresses or full URLs
        return WebClient.builder();
    }
}

