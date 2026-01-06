package com.enterprise.gateway.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Token Refresh Service
 * <p>
 * Handles automatic token refresh with Keycloak using refresh tokens.
 * This is a critical component of the BFF pattern for maintaining user sessions
 * without requiring re-authentication.
 * <p>
 * Flow:
 * 1. Detect access token expiration
 * 2. Use refresh token to obtain new access token from Keycloak
 * 3. Update session in Redis
 * 4. Invalidate L1 cache
 * <p>
 * Error Handling:
 * - If refresh fails (refresh token expired), session is invalidated
 * - User must re-authenticate via /auth/login
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
/**
 * Token Refresh Service
 * <p>
 * Handles automatic token refresh with Keycloak using refresh tokens.
 * This is a critical component of the BFF pattern for maintaining user sessions
 * without requiring re-authentication.
 * <p>
 * Uses standard (non-load-balanced) WebClient.Builder for direct HTTP calls
 * to Keycloak token endpoint (full URL, not service discovery).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    /**
     * Use standard WebClient.Builder (not @LoadBalanced) for direct HTTP calls to Keycloak.
     * Keycloak token endpoint is accessed via full URL, not service discovery.
     */
    @Qualifier("standardWebClientBuilder")
    private final WebClient.Builder standardWebClientBuilder;
    private final SessionService sessionService;

    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    /**
     * Refresh access token for a session.
     * <p>
     * Uses OAuth2 refresh_token grant to obtain new access token from Keycloak.
     *
     * @param sessionId Session ID
     * @param refreshToken Current refresh token
     * @return Mono that completes when tokens are refreshed
     */
    public Mono<Void> refreshAccessToken(String sessionId, String refreshToken) {
        log.info("Refreshing access token for session: {}", sessionId);

        return standardWebClientBuilder.build()
                .post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken)
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(response -> {
                    log.info("Successfully refreshed tokens for session: {}", sessionId);

                    // Update session with new tokens
                    return sessionService.updateTokens(
                            sessionId,
                            response.getAccessToken(),
                            response.getRefreshToken() != null ? response.getRefreshToken() : refreshToken
                    );
                })
                .doOnError(error -> {
                    log.error("Failed to refresh token for session: {}", sessionId, error);
                    // Invalidate session on refresh failure
                    sessionService.deleteSession(sessionId).subscribe();
                });
    }

    /**
     * Refresh access token if needed (checks expiration).
     *
     * @param sessionId Session ID
     * @return Mono that completes when refresh is done (or not needed)
     */
    public Mono<Void> refreshIfNeeded(String sessionId) {
        return sessionService.getSession(sessionId)
                .flatMap(session -> {
                    if (session.isAccessTokenExpired()) {
                        log.info("Access token expired for session {}, triggering refresh", sessionId);
                        return refreshAccessToken(sessionId, session.getRefreshToken());
                    } else {
                        log.debug("Access token still valid for session {}", sessionId);
                        return Mono.empty();
                    }
                });
    }

    /**
     * Token Response from Keycloak.
     */
    @lombok.Data
    public static class TokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_expires_in")
        private Long refreshExpiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("id_token")
        private String idToken;

        @JsonProperty("not-before-policy")
        private Long notBeforePolicy;

        @JsonProperty("session_state")
        private String sessionState;

        @JsonProperty("scope")
        private String scope;
    }
}
