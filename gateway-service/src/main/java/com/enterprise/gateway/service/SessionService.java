package com.enterprise.gateway.service;

import com.enterprise.gateway.model.UserSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Session Management Service (BFF Pattern with L1 Caching)
 *
 * Responsibilities:
 * - Create session from OAuth2 tokens
 * - Store session in Redis (L2)
 * - Cache session tokens in Caffeine (L1)
 * - Retrieve session by SESSION_ID
 * - Validate session
 * - Refresh access token
 * - Delete session (logout)
 *
 * Session Storage:
 * - L1 (Caffeine): sessionId -> accessToken (60s TTL, 100K max)
 * - L2 (Redis): "session:{sessionId}" -> UserSession JSON (24h TTL)
 *
 * Performance:
 * - L1 Hit: ~1µs latency
 * - L2 Hit: ~1ms latency
 * - Target Cache Hit Rate: > 95%
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;
    private final Cache<String, String> sessionTokenCache;  // L1 Caffeine cache

    private static final String SESSION_PREFIX = "session:";
    private static final String ONLINE_PREFIX = "online:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration ONLINE_TTL = Duration.ofMinutes(5); // 5 minutes TTL for online status

    /**
     * Create session from access token
     *
     * @param accessToken JWT access token
     * @param refreshToken Refresh token
     * @return Session ID
     */
    public Mono<String> createSession(String accessToken, String refreshToken) {
        return jwtDecoder.decode(accessToken)
            .flatMap(jwt -> {
                String sessionId = UUID.randomUUID().toString();

                UserSession session = UserSession.builder()
                    .sessionId(sessionId)
                    .userId(jwt.getSubject())
                    .username(jwt.getClaimAsString("preferred_username"))
                    .email(jwt.getClaimAsString("email"))
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresAt(jwt.getExpiresAt())
                    .refreshTokenExpiresAt(calculateRefreshTokenExpiry(jwt))
                    .createdAt(Instant.now())
                    .lastAccessedAt(Instant.now())
                    .build();

                return saveSession(session)
                    .then(incrementCcu(session.getUserId()))
                    .thenReturn(sessionId);
            })
            .doOnSuccess(sessionId -> log.info("Session created: {}", sessionId))
            .doOnError(error -> log.error("Failed to create session", error));
    }

    /**
     * Get session by ID
     *
     * @param sessionId Session ID
     * @return UserSession or empty if not found
     */
    public Mono<UserSession> getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;

        return redisTemplate.opsForValue().get(key)
            .flatMap(json -> {
                try {
                    UserSession session = objectMapper.readValue(json, UserSession.class);

                    if (!session.isValid()) {
                        log.warn("Session {} is expired", sessionId);
                        return deleteSession(sessionId).then(Mono.empty());
                    }

                    // Update last accessed time
                    session.updateLastAccessed();
                    return saveSession(session).thenReturn(session);

                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize session", e);
                    return Mono.empty();
                }
            })
            .doOnNext(session -> log.debug("Session retrieved: {}", sessionId));
    }

    /**
     * Get access token from session (with L1 caching)
     * <p>
     * Performance optimization for BFF pattern:
     * 1. Check L1 cache (Caffeine) - ~1µs latency
     * 2. If miss, check L2 (Redis) - ~1ms latency
     * 3. Cache result in L1 for 60 seconds
     *
     * @param sessionId Session ID
     * @return Access token or empty if session not found
     */
    public Mono<String> getAccessToken(String sessionId) {
        // L1 Cache lookup
        String cachedToken = sessionTokenCache.getIfPresent(sessionId);
        if (cachedToken != null) {
            log.trace("L1 cache hit for session: {}", sessionId);
            return Mono.just(cachedToken);
        }

        log.trace("L1 cache miss for session: {}, checking Redis", sessionId);

        // L2 Redis lookup
        return getSession(sessionId)
            .map(session -> {
                String accessToken = session.getAccessToken();
                // Store in L1 cache for next request
                sessionTokenCache.put(sessionId, accessToken);
                log.trace("Cached access token in L1 for session: {}", sessionId);
                return accessToken;
            });
    }

    /**
     * Validate session and get user info
     *
     * @param sessionId Session ID
     * @return JWT from session or error if invalid
     */
    public Mono<Jwt> validateSession(String sessionId) {
        return getSession(sessionId)
            .flatMap(session -> {
                if (session.isAccessTokenExpired()) {
                    log.info("Access token expired for session {}, need refresh", sessionId);
                    return Mono.error(new SessionExpiredException("Access token expired"));
                }

                return jwtDecoder.decode(session.getAccessToken());
            });
    }

    /**
     * Update access token in session (after refresh)
     *
     * IMPORTANT: Invalidates L1 cache to ensure consistency across gateway instances.
     *
     * @param sessionId Session ID
     * @param newAccessToken New access token
     * @param newRefreshToken New refresh token (optional)
     */
    public Mono<Void> updateTokens(String sessionId, String newAccessToken, String newRefreshToken) {
        return getSession(sessionId)
            .flatMap(session -> jwtDecoder.decode(newAccessToken)
                .flatMap(jwt -> {
                    session.setAccessToken(newAccessToken);
                    session.setAccessTokenExpiresAt(jwt.getExpiresAt());

                    if (newRefreshToken != null) {
                        session.setRefreshToken(newRefreshToken);
                        session.setRefreshTokenExpiresAt(calculateRefreshTokenExpiry(jwt));
                    }

                    // Invalidate L1 cache (new token will be cached on next access)
                    sessionTokenCache.invalidate(sessionId);
                    log.debug("Invalidated L1 cache for session: {}", sessionId);

                    return saveSession(session);
                })
            )
            .doOnSuccess(v -> log.info("Tokens updated for session {}", sessionId))
            .then();
    }

    /**
     * Delete session (logout)
     *
     * Removes session from both L1 (Caffeine) and L2 (Redis).
     *
     * @param sessionId Session ID
     */
    public Mono<Boolean> deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;

        // Invalidate L1 cache
        sessionTokenCache.invalidate(sessionId);
        log.debug("Invalidated L1 cache for session: {}", sessionId);

        // Get session first to retrieve userId for CCU tracking
        return redisTemplate.opsForValue().get(key)
            .flatMap(json -> {
                try {
                    UserSession session = objectMapper.readValue(json, UserSession.class);
                    // Decrement CCU before deleting session
                    return decrementCcu(session.getUserId())
                        .then(redisTemplate.delete(key))
                        .map(deleted -> deleted > 0);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize session for CCU tracking", e);
                    // Still delete the session even if deserialization fails
                    return redisTemplate.delete(key).map(deleted -> deleted > 0);
                }
            })
            .switchIfEmpty(redisTemplate.delete(key).map(deleted -> deleted > 0))
            .doOnSuccess(deleted -> {
                if (deleted) {
                    log.info("Session deleted: {}", sessionId);
                } else {
                    log.warn("Session not found for deletion: {}", sessionId);
                }
            });
    }

    /**
     * Save session to Redis
     */
    private Mono<Void> saveSession(UserSession session) {
        String key = SESSION_PREFIX + session.getSessionId();

        try {
            String json = objectMapper.writeValueAsString(session);

            return redisTemplate.opsForValue()
                .set(key, json, SESSION_TTL)
                .then()
                .doOnSuccess(v -> log.debug("Session saved: {}", session.getSessionId()))
                .doOnError(error -> log.error("Failed to save session", error));

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session", e);
            return Mono.error(e);
        }
    }

    /**
     * Calculate refresh token expiry
     * Typically refresh token expires in 30 days
     */
    private Instant calculateRefreshTokenExpiry(Jwt jwt) {
        // If JWT has refresh_expires_in claim, use it
        // Otherwise default to 30 days from now
        return Instant.now().plus(Duration.ofDays(30));
    }

    /**
     * Increment CCU counter when user logs in
     * Sets online:{userId} key with TTL to track active users
     *
     * @param userId User ID
     * @return Mono<Void>
     */
    private Mono<Void> incrementCcu(String userId) {
        String key = ONLINE_PREFIX + userId;
        return redisTemplate.opsForValue()
            .set(key, "1", ONLINE_TTL)
            .then()
            .doOnSuccess(v -> log.debug("Incremented CCU for user: {}", userId))
            .doOnError(error -> log.error("Failed to increment CCU for user: {}", userId, error))
            .onErrorResume(error -> Mono.empty()); // Don't fail session creation if CCU tracking fails
    }

    /**
     * Decrement CCU counter when user logs out
     * Removes online:{userId} key
     *
     * @param userId User ID
     * @return Mono<Void>
     */
    private Mono<Void> decrementCcu(String userId) {
        String key = ONLINE_PREFIX + userId;
        return redisTemplate.delete(key)
            .then()
            .doOnSuccess(v -> log.debug("Decremented CCU for user: {}", userId))
            .doOnError(error -> log.error("Failed to decrement CCU for user: {}", userId, error))
            .onErrorResume(error -> Mono.empty()); // Don't fail session deletion if CCU tracking fails
    }

    /**
     * Exception for expired session
     */
    public static class SessionExpiredException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public SessionExpiredException(String message) {
            super(message);
        }
    }
}
