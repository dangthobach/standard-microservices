package com.enterprise.gateway.filter;

import com.enterprise.gateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * JWT Enrichment Global Filter (BFF Pattern)
 * <p>
 * This filter is the heart of the BFF authentication workflow.
 * It intercepts ALL downstream requests and:
 * 1. Extracts SESSION_ID from cookie
 * 2. Looks up access token (L1 cache -> L2 Redis)
 * 3. Attaches "Authorization: Bearer {token}" header to downstream requests
 * 4. Updates "online:{userId}" key for CCU tracking (heartbeat)
 * <p>
 * Flow:
 * <pre>
 * Client Request (Cookie: SESSION_ID=xyz)
 *   ↓
 * [This Filter]
 *   ├─> Check L1 Cache (SESSION_ID -> Access Token) ~1µs
 *   ├─> If Miss: Check L2 Redis ~1ms
 *   ├─> Attach "Authorization: Bearer {token}"
 *   └─> Update "online:{userId}" with 2-3 min TTL (async)
 *   ↓
 * Downstream Microservice (receives JWT)
 * </pre>
 * <p>
 * Order: -1 (runs before other filters)
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtEnrichmentFilter implements GlobalFilter, Ordered {

    private final SessionService sessionService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${ccu.metrics.online.ttl-minutes:2}")
    private int onlineTtlMinutes;

    private static final String SESSION_COOKIE_NAME = "SESSION_ID";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ONLINE_PREFIX = "online:";
    private static final int ORDER = -1;  // Run before other filters

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            log.trace("Skipping JWT enrichment for public endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Extract SESSION_ID from cookie
        String sessionId = extractSessionId(exchange);

        if (sessionId == null) {
            log.trace("No session cookie found for request: {}", path);
            // Allow request to continue (might be using direct JWT auth)
            return chain.filter(exchange);
        }

        // Lookup session (to get userId for CCU tracking)
        return sessionService.getSession(sessionId)
                .flatMap(session -> {
                    // Mutate request to add Authorization header
                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + session.getAccessToken())
                            .build();

                    log.debug("Enriched request with JWT for session: {} -> {}", sessionId, path);

                    // Update online status (heartbeat) - Fire and forget (async)
                    // This does NOT block the request path
                    updateOnlineStatus(session.getUserId())
                            .subscribe(
                                    result -> log.trace("Updated online status for user: {}", session.getUserId()),
                                    error -> log.warn("Failed to update online status for user: {}", session.getUserId(), error)
                            );

                    // Continue filter chain with enriched request
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Session not found or expired
                    log.warn("Session not found or expired: {} for request: {}", sessionId, path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }))
                .onErrorResume(error -> {
                    // Error during session lookup
                    log.error("Error enriching request with JWT for session: {}", sessionId, error);
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * Extract SESSION_ID from cookie.
     *
     * @param exchange Server web exchange
     * @return Session ID or null if not found
     */
    private String extractSessionId(ServerWebExchange exchange) {
        HttpCookie sessionCookie = exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME);

        if (sessionCookie != null) {
            return sessionCookie.getValue();
        }

        // Fallback: Check X-Session-Id header (for non-browser clients)
        return exchange.getRequest().getHeaders().getFirst("X-Session-Id");
    }

    /**
     * Check if the endpoint is public (no authentication required).
     * <p>
     * Public endpoints:
     * - /actuator/** (health, metrics)
     * - /auth/** (login, logout, callback)
     * - /oauth2/** (OAuth2 flow)
     * - /public/** (static content)
     *
     * @param path Request path
     * @return true if endpoint is public
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/health/") ||
               path.startsWith("/auth/") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/login/") ||
               path.startsWith("/public/");
    }

    /**
     * Update online status for CCU tracking (Heartbeat mechanism).
     * <p>
     * This method sets/refreshes a short-lived Redis key "online:{userId}"
     * with a 2-3 minute TTL. This allows accurate real-time CCU tracking
     * by counting only users who have made a request within the TTL window.
     * <p>
     * Architecture:
     * - Key Pattern: "online:{userId}"
     * - Value: "1" (presence marker)
     * - TTL: 2-3 minutes (configurable via ccu.metrics.online.ttl-minutes)
     * <p>
     * Why This Works:
     * 1. Every authenticated request refreshes the online key
     * 2. If user closes browser without logout, key expires after 2-3 min
     * 3. CcuMetricsScheduler scans "online:*" to count active users
     * 4. Result: Accurate real-time CCU (not just valid sessions)
     * <p>
     * Performance:
     * - Redis SET operation: ~0.5ms
     * - Executed asynchronously (fire-and-forget)
     * - Zero impact on request latency
     * - Even if this fails, request continues normally
     *
     * @param userId User ID from session
     * @return Mono<Boolean> indicating success/failure
     */
    private Mono<Boolean> updateOnlineStatus(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Mono.just(false);
        }

        String onlineKey = ONLINE_PREFIX + userId;
        Duration ttl = Duration.ofMinutes(onlineTtlMinutes);

        return redisTemplate.opsForValue()
                .set(onlineKey, "1", ttl)
                .doOnSuccess(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.trace("✅ Updated online status: {} (TTL: {}m)", onlineKey, onlineTtlMinutes);
                    }
                })
                .onErrorResume(error -> {
                    // Don't fail the request if online status update fails
                    log.warn("⚠️ Failed to update online status for user: {}", userId, error);
                    return Mono.just(false);
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
