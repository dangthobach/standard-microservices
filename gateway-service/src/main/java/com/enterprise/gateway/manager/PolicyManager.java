package com.enterprise.gateway.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Policy Manager
 * <p>
 * Loads and caches Dynamic Authorization Policies from IAM Service.
 * Matches incoming requests against these policies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyManager {

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    // Thread-safe list for active policies
    private final List<EndpointPolicy> policies = new CopyOnWriteArrayList<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl("lb://iam-service").build();
        refreshPolicies().subscribe();
    }

    /**
     * Refresh policies from IAM (Scheduled every 5 min + Webhook support)
     */
    @Scheduled(fixedRate = 300_000)
    public Mono<Void> refreshPolicies() {
        log.debug("Refreshing Authorization Policies...");
        return webClient.get()
                .uri("/api/internal/policies")
                .retrieve()
                .bodyToFlux(EndpointPolicy.class)
                .collectList()
                .doOnNext(newPolicies -> {
                    policies.clear();
                    policies.addAll(newPolicies);
                    log.info("Loaded {} authorization policies", policies.size());
                })
                .doOnError(e -> log.error("Failed to refresh policies", e))
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
        for (EndpointPolicy policy : policies) {
            // Match Method (Exact or Wildcard)
            if (!policy.getMethod().equals("*") && !policy.getMethod().equalsIgnoreCase(method)) {
                continue;
            }
            // Match Path
            if (pathMatcher.match(policy.getPattern(), path)) {
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
