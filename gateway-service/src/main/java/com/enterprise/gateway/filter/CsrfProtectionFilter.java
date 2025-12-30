package com.enterprise.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * CSRF Protection Filter (BFF Pattern)
 * <p>
 * Protects against Cross-Site Request Forgery attacks by requiring a custom
 * header for mutating requests (POST, PUT, DELETE, PATCH).
 * <p>
 * Security Strategy:
 * 1. SESSION_ID cookie has SameSite=Strict attribute
 * 2. Mutating requests MUST include X-XSRF-TOKEN or X-Requested-With header
 * 3. Browsers prevent external sites from adding custom headers
 * <p>
 * Why this works:
 * - Attacker can trigger request from external site
 * - Browser auto-sends SESSION_ID cookie
 * - But browser BLOCKS custom headers from external origins
 * - Request fails CSRF check -> 403 Forbidden
 * <p>
 * Frontend Implementation:
 * <pre>
 * // Angular automatically adds X-XSRF-TOKEN
 * // Or manually add header:
 * http.post('/api/users', data, {
 *   headers: { 'X-Requested-With': 'XMLHttpRequest' }
 * })
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html">OWASP CSRF Prevention</a>
 */
@Slf4j
@Component
public class CsrfProtectionFilter implements GlobalFilter, Ordered {

    private static final Set<HttpMethod> MUTATING_METHODS = Set.of(
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.PATCH
    );

    private static final Set<String> CSRF_HEADERS = Set.of(
            "X-XSRF-TOKEN",      // Standard CSRF token header
            "X-Requested-With",  // AJAX header (simple alternative)
            "X-CSRF-TOKEN"       // Alternative CSRF token header
    );

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/callback",
            "/auth/session",
            "/oauth2/",
            "/login/"
    );

    private static final int ORDER = -10; // Run before JWT enrichment

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();

        // Only check CSRF for mutating methods
        if (!MUTATING_METHODS.contains(method)) {
            return chain.filter(exchange);
        }

        // Skip CSRF check for public endpoints
        if (isPublicPath(path)) {
            log.trace("Skipping CSRF check for public path: {}", path);
            return chain.filter(exchange);
        }

        // Check for CSRF protection header
        boolean hasCsrfHeader = CSRF_HEADERS.stream()
                .anyMatch(header -> exchange.getRequest().getHeaders().containsKey(header));

        if (!hasCsrfHeader) {
            log.warn("CSRF check failed: Missing required header for {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(
                            "{\"error\":\"CSRF_PROTECTION\",\"message\":\"Missing CSRF protection header. Add X-XSRF-TOKEN or X-Requested-With header.\"}".getBytes()
                    ))
            );
        }

        log.trace("CSRF check passed for {} {}", method, path);
        return chain.filter(exchange);
    }

    /**
     * Check if path is public (no CSRF check required).
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
