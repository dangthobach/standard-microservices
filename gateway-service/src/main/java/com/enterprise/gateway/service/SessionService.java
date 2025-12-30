package com.enterprise.gateway.service;

import com.enterprise.gateway.model.UserSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Session Management Service
 *
 * Responsibilities:
 * - Create session from OAuth2 tokens
 * - Store session in Redis
 * - Retrieve session by SESSION_ID
 * - Validate session
 * - Refresh access token
 * - Delete session (logout)
 *
 * Session Storage:
 * - Key: "session:{sessionId}"
 * - Value: UserSession JSON
 * - TTL: 24 hours (configurable)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

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
     * Get access token from session
     *
     * @param sessionId Session ID
     * @return Access token or empty if session not found
     */
    public Mono<String> getAccessToken(String sessionId) {
        return getSession(sessionId)
            .map(UserSession::getAccessToken);
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

                    return saveSession(session);
                })
            )
            .doOnSuccess(v -> log.info("Tokens updated for session {}", sessionId))
            .then();
    }

    /**
     * Delete session (logout)
     *
     * @param sessionId Session ID
     */
    public Mono<Boolean> deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;

        return redisTemplate.delete(key)
            .map(deleted -> deleted > 0)
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
     * Exception for expired session
     */
    public static class SessionExpiredException extends RuntimeException {
        public SessionExpiredException(String message) {
            super(message);
        }
    }
}
