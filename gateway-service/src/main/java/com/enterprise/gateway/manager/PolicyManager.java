package com.enterprise.gateway.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Policy Manager
 * <p>
 * Loads and caches Dynamic Authorization Policies from IAM Service.
 * Matches incoming requests against these policies.
 * <p>
 * Uses LoadBalanced WebClient for service discovery and load balancing
 * across multiple IAM service instances.
 */
@Slf4j
@Component
public class PolicyManager {

    private final WebClient webClient;

    // Thread-safe list for active policies
    private final List<EndpointPolicy> policies = new CopyOnWriteArrayList<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Constructor with LoadBalanced WebClient.Builder for service discovery.
     * <p>
     * Service name will be resolved via Consul, supporting multiple instances
     * with automatic load balancing.
     *
     * @param loadBalancedWebClientBuilder LoadBalanced WebClient builder
     * @param iamServiceName Service name (default: "iam-service")
     */
    public PolicyManager(
            @LoadBalanced WebClient.Builder loadBalancedWebClientBuilder,
            @Value("${iam.service.name:iam-service}") String iamServiceName) {
        // Use lb:// scheme for LoadBalancer to resolve service name from Consul
        // This enables load balancing across multiple IAM service instances
        String serviceUrl = "lb://" + iamServiceName;
        this.webClient = loadBalancedWebClientBuilder.baseUrl(serviceUrl).build();
        log.info("✅ PolicyManager configured with LoadBalanced WebClient for service: {}", serviceUrl);
    }

    /**
     * Initialize policy refresh after application is ready.
     * This ensures Consul and downstream services are available before attempting connection.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // Delay initial refresh to allow services to register in Consul
        log.info("PolicyManager: Scheduling initial policy refresh after application startup...");
        refreshPoliciesWithRetry()
                .delaySubscription(Duration.ofSeconds(10)) // Wait 10 seconds for services to register
                .subscribe(
                        null,
                        error -> log.warn("Initial policy refresh failed (will retry on schedule): {}", error.getMessage()),
                        () -> log.info("✅ Initial policy refresh completed successfully")
                );
    }

    /**
     * Refresh policies from IAM (Scheduled every 5 min + Webhook support)
     */
    @Scheduled(fixedRate = 300_000)
    public Mono<Void> refreshPolicies() {
        return refreshPoliciesWithRetry();
    }

    /**
     * Refresh policies with retry logic and timeout handling.
     * This method is resilient to temporary service discovery failures.
     */
    private Mono<Void> refreshPoliciesWithRetry() {
        log.debug("Refreshing Authorization Policies...");
        return webClient.get()
                .uri("/api/internal/policies")
                .retrieve()
                .bodyToFlux(EndpointPolicy.class)
                .collectList()
                .timeout(Duration.ofSeconds(10)) // 10 second timeout
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(retrySignal -> 
                                log.debug("Retrying policy refresh (attempt {}/3): {}", 
                                        retrySignal.totalRetries() + 1, 
                                        retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.warn("Policy refresh failed after {} retries", retrySignal.totalRetries());
                            return retrySignal.failure();
                        }))
                .doOnNext(newPolicies -> {
                    policies.clear();
                    policies.addAll(newPolicies);
                    log.info("✅ Loaded {} authorization policies", policies.size());
                })
                .doOnError(e -> log.warn("Failed to refresh policies (will retry on next schedule): {}", 
                        e.getMessage()))
                .onErrorResume(e -> {
                    // Don't fail completely - keep existing policies and retry later
                    log.debug("Policy refresh error details", e);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Find matching policy for a request.
     *
     * @param method HTTP Method
     * @param path   Request Path
     * @return Matching Policy or NULL
     */
    public EndpointPolicy findPolicy(String method, String path) {
        if (method == null || path == null) {
            return null;
        }
        
        for (EndpointPolicy policy : policies) {
            // Match Method (Exact or Wildcard)
            String policyMethod = policy.getMethod();
            if (policyMethod == null || 
                (!policyMethod.equals("*") && !policyMethod.equalsIgnoreCase(method))) {
                continue;
            }
            // Match Path
            String pattern = policy.getPattern();
            if (pattern != null && pathMatcher.match(pattern, path)) {
                return policy;
            }
        }
        return null; // No protection rule found
    }

    @Data
    public static class EndpointPolicy {
        private String pattern;
        private String method;
        private String permissionCode;
        @JsonProperty("public")
        private boolean isPublic;
        private Integer priority;
    }
}
