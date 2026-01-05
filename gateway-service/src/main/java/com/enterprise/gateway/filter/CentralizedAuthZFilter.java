package com.enterprise.gateway.filter;

import com.enterprise.gateway.manager.PolicyManager;
import com.enterprise.gateway.service.AuthZService;
import com.enterprise.gateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Centralized AuthZ Filter
 * <p>
 * Enforces Dynamic Authorization Policies.
 * Execution Order: After JwtEnrichmentFilter (-1).
 * <p>
 * Logic:
 * 1. Match Request -> Policy
 * 2. If No Policy or Public -> Proceed
 * 3. Extract User ID from Session (via SessionService cache)
 * 4. Check Permission (via AuthZService cache)
 * 5. Allow or Deny
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CentralizedAuthZFilter implements GlobalFilter, Ordered {

    private final PolicyManager policyManager;
    private final AuthZService authZService;
    private final SessionService sessionService;

    // Run AFTER JwtEnrichment (Order -1)
    // We set this to 0 ensures it runs after enrichment
    private static final int ORDER = 0;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // 1. Find Matching Policy
        PolicyManager.EndpointPolicy policy = policyManager.findPolicy(method, path);

        if (policy == null) {
            // No rule defined - Fail Safe: Log warning and Allow (or Deny depending on
            // strictness)
            // For now, we allow logical endpoints not in DB (e.g. static resources)
            return chain.filter(exchange);
        }

        if (policy.isPublic()) {
            // Explicitly Public -> Permit All
            return chain.filter(exchange);
        }

        // 2. Identify User
        String sessionId = extractSessionId(exchange);
        if (sessionId == null) {
            // Protected resource but no session -> 401
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 3. Check Permissions
        return sessionService.getSession(sessionId)
                .flatMap(session -> authZService.hasPermission(session.getUserId(), policy.getPermissionCode())
                        .flatMap(hasPerm -> {
                            if (Boolean.TRUE.equals(hasPerm)) {
                                // Authorized -> Add Header for debug/audit
                                exchange.getRequest().mutate()
                                        .header("X-AuthZ-Perm", policy.getPermissionCode())
                                        .build();
                                return chain.filter(exchange);
                            } else {
                                // Forbidden -> 403
                                log.warn("Access Denied for user: {} to {} [Required: {}]",
                                        session.getUserId(), path, policy.getPermissionCode());
                                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                                return exchange.getResponse().setComplete();
                            }
                        }))
                .switchIfEmpty(Mono.defer(() -> {
                    // Session invalid/expired -> 401
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }));
    }

    private String extractSessionId(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("SESSION_ID");
        return cookie != null ? cookie.getValue() : exchange.getRequest().getHeaders().getFirst("X-Session-Id");
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
