package com.enterprise.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Gateway WebClient Configuration
 * <p>
 * This configuration extends the common-lib WebClientConfiguration for
 * Gateway-specific needs, primarily adding LoadBalancer support.
 * <p>
 * Architecture (extends common-lib):
 * 
 * <pre>
 * common-lib
 * ├── WebClientProperties    → Shared externalized config (timeouts, pool)
 * └── HttpClient Bean        → Shared HTTP client with timeouts
 *         ↓
 * gateway-service (this class)
 * ├── LoadBalanced Builder   → For service discovery (iam-service, etc.)
 * └── Standard Builder       → For direct calls (Keycloak, health checks)
 * </pre>
 * <p>
 * Two types of WebClient Builders:
 * <p>
 * 1. {@code @LoadBalanced} WebClient.Builder (Primary):
 * - For inter-service communication using service discovery (Consul/K8s)
 * - Resolves service names like "iam-service" via service discovery
 * - Use with URLs like: "lb://iam-service" or "http://iam-service"
 * - Used by: PolicyManager, AuthZService, UserRoleService
 * <p>
 * 2. Standard WebClient.Builder (named "standardWebClientBuilder"):
 * - For direct HTTP calls to specific endpoints (bypasses service discovery)
 * - Use with full URLs like: "http://keycloak:8080/auth/..."
 * - Used by: DownstreamServicesHealthIndicator, TokenRefreshService
 * <p>
 * This separation prevents LoadBalancer from trying to resolve IP addresses
 * or direct URLs as service names, which causes "No servers available" errors.
 *
 * @author Enterprise Team
 * @since 1.0.0
 * @see com.enterprise.common.config.WebClientConfiguration
 * @see com.enterprise.common.config.WebClientProperties
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayWebClientConfiguration {

    /**
     * Shared HttpClient from common-lib with configured timeouts and connection
     * pool.
     * This ensures consistent configuration across all WebClient instances.
     */
    private final HttpClient baseHttpClient;

    /**
     * LoadBalanced WebClient.Builder with service discovery support.
     * <p>
     * This bean enables WebClient to resolve service names from Consul
     * and load balance across multiple service instances.
     * <p>
     * Marked as @Primary so it's the default when injecting WebClient.Builder
     * without qualifiers.
     * <p>
     * Usage:
     * 
     * <pre>
     * {
     *     &#64;code
     *     &#64;Autowired
     *     @LoadBalanced
     *     private WebClient.Builder loadBalancedWebClientBuilder;
     *
     *     WebClient client = loadBalancedWebClientBuilder
     *             .baseUrl("lb://iam-service")
     *             .build();
     * }
     * </pre>
     *
     * @param loadBalancerFilter The reactive load balancer filter from Spring Cloud
     * @return LoadBalanced WebClient.Builder with shared HttpClient config
     */
    @Bean
    @Primary
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(
            ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter) {
        log.info("✅ Configuring LoadBalanced WebClient.Builder with service discovery");

        if (loadBalancerFilter == null) {
            throw new IllegalStateException(
                    "ReactorLoadBalancerExchangeFilterFunction is required for LoadBalanced WebClient. " +
                            "Ensure spring-cloud-starter-loadbalancer is on the classpath.");
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(baseHttpClient))
                .filter(loadBalancerFilter);
    }

    /**
     * Standard WebClient.Builder for direct HTTP calls (bypasses service
     * discovery).
     * <p>
     * This bean is used for direct HTTP calls to specific endpoints,
     * such as:
     * - Keycloak token endpoint (external IdP)
     * - Health checks using Docker DNS or K8s service addresses
     * - Any external APIs not registered in service discovery
     * <p>
     * Uses the same HttpClient from common-lib to ensure consistent
     * timeout and connection pool configuration.
     * <p>
     * Usage:
     * 
     * <pre>
     * {@code
     * @Autowired
     * @Qualifier("standardWebClientBuilder")
     * private WebClient.Builder standardWebClientBuilder;
     *
     * WebClient client = standardWebClientBuilder.build();
     * client.get()
     *         .uri("http://keycloak:8080/auth/realms/master")
     *         .retrieve()
     *         .bodyToMono(String.class);
     * }
     * </pre>
     *
     * @return Standard WebClient.Builder with shared HttpClient config
     */
    @Bean(name = "standardWebClientBuilder")
    public WebClient.Builder standardWebClientBuilder() {
        log.info("✅ Configuring Standard WebClient.Builder for direct HTTP calls (bypasses service discovery)");

        // Use shared HttpClient from common-lib to ensure consistent config
        // (timeouts, connection pool, etc.)
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(baseHttpClient));
    }
}
