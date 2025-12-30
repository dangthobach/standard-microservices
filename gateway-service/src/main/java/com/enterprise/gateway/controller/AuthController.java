package com.enterprise.gateway.controller;

import com.enterprise.gateway.model.UserSession;
import com.enterprise.gateway.service.SessionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Authentication Controller
 *
 * Endpoints:
 * - POST /auth/session - Create session from JWT token
 * - GET /auth/me - Get current user info
 * - POST /auth/refresh - Refresh access token
 * - POST /auth/logout - Logout and delete session
 * - GET /auth/login/success - OAuth2 login success callback
 *
 * Flow:
 * 1. Frontend initiates OAuth2 login via Spring Security
 * 2. User redirected to Keycloak
 * 3. After successful login, redirected to /auth/login/success
 * 4. Gateway creates SESSION_ID and returns to frontend
 * 5. Frontend stores SESSION_ID
 * 6. Subsequent requests use SESSION_ID cookie
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SessionService sessionService;

    private static final String SESSION_COOKIE_NAME = "SESSION_ID";
    private static final Duration COOKIE_MAX_AGE = Duration.ofHours(24);

    /**
     * OAuth2 login success callback
     *
     * Called after successful OAuth2 authentication
     * Creates session and returns SESSION_ID
     *
     * @param authorizedClient OAuth2 authorized client with tokens
     * @param oidcUser OIDC user information
     * @param response HTTP response to set cookie
     * @return Session info
     */
    @GetMapping("/login/success")
    public Mono<ResponseEntity<LoginSuccessResponse>> loginSuccess(
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
        @AuthenticationPrincipal OidcUser oidcUser,
        ServerHttpResponse response
    ) {
        log.info("OAuth2 login successful for user: {}", oidcUser.getPreferredUsername());

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String refreshToken = authorizedClient.getRefreshToken() != null ?
            authorizedClient.getRefreshToken().getTokenValue() : null;

        return sessionService.createSession(accessToken, refreshToken)
            .map(sessionId -> {
                // Set SESSION_ID cookie
                ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, sessionId)
                    .httpOnly(true)
                    .secure(true)  // Use HTTPS in production
                    .sameSite("Lax")
                    .maxAge(COOKIE_MAX_AGE)
                    .path("/")
                    .build();

                response.addCookie(cookie);

                LoginSuccessResponse loginResponse = new LoginSuccessResponse(
                    sessionId,
                    oidcUser.getSubject(),
                    oidcUser.getPreferredUsername(),
                    oidcUser.getEmail(),
                    "Login successful"
                );

                log.info("Session created for user {}: {}", oidcUser.getPreferredUsername(), sessionId);

                return ResponseEntity.ok(loginResponse);
            });
    }

    /**
     * Create session from JWT token
     *
     * Alternative endpoint for SPA that handles OAuth2 flow itself
     * Frontend exchanges authorization code for token, then creates session
     *
     * @param request Create session request with access token
     * @param response HTTP response to set cookie
     * @return Session info
     */
    @PostMapping("/session")
    public Mono<ResponseEntity<SessionResponse>> createSession(
        @RequestBody CreateSessionRequest request,
        ServerHttpResponse response
    ) {
        log.info("Creating session from access token");

        return sessionService.createSession(request.getAccessToken(), request.getRefreshToken())
            .map(sessionId -> {
                // Set SESSION_ID cookie
                ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, sessionId)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .maxAge(COOKIE_MAX_AGE)
                    .path("/")
                    .build();

                response.addCookie(cookie);

                SessionResponse sessionResponse = new SessionResponse(
                    sessionId,
                    "Session created successfully"
                );

                log.info("Session created: {}", sessionId);

                return ResponseEntity.ok(sessionResponse);
            })
            .onErrorResume(error -> {
                log.error("Failed to create session", error);
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SessionResponse(null, "Invalid token")));
            });
    }

    /**
     * Get current user information
     *
     * Requires SESSION_ID cookie or Authorization header
     *
     * @param exchange Server web exchange to extract session ID
     * @return User information
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<UserInfoResponse>> getCurrentUser(ServerWebExchange exchange) {
        return extractSessionId(exchange)
            .flatMap(sessionService::getSession)
            .map(session -> {
                UserInfoResponse userInfo = new UserInfoResponse(
                    session.getUserId(),
                    session.getUsername(),
                    session.getEmail(),
                    !session.isAccessTokenExpired()
                );

                return ResponseEntity.ok(userInfo);
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new UserInfoResponse(null, null, null, false))));
    }

    /**
     * Refresh access token
     *
     * Uses refresh token to get new access token
     *
     * @param exchange Server web exchange to extract session ID
     * @return Success message
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<MessageResponse>> refreshToken(ServerWebExchange exchange) {
        return extractSessionId(exchange)
            .flatMap(sessionId -> sessionService.getSession(sessionId)
                .flatMap(session -> {
                    if (session.isRefreshTokenExpired()) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new MessageResponse("Refresh token expired, please login again")));
                    }

                    // TODO: Call Keycloak token endpoint to refresh
                    // For now, return error
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .body(new MessageResponse("Token refresh not implemented yet")));
                })
            )
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Session not found"))));
    }

    /**
     * Logout
     *
     * Deletes session and clears cookie
     *
     * @param exchange Server web exchange to extract session ID
     * @param response HTTP response to clear cookie
     * @return Logout message
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<MessageResponse>> logout(
        ServerWebExchange exchange,
        ServerHttpResponse response
    ) {
        return extractSessionId(exchange)
            .flatMap(sessionId -> sessionService.deleteSession(sessionId)
                .map(deleted -> {
                    // Clear cookie
                    ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .maxAge(Duration.ZERO)
                        .path("/")
                        .build();

                    response.addCookie(cookie);

                    log.info("User logged out, session deleted: {}", sessionId);

                    return ResponseEntity.ok(new MessageResponse("Logout successful"));
                })
            )
            .switchIfEmpty(Mono.just(ResponseEntity.ok(new MessageResponse("No active session"))));
    }

    /**
     * Extract SESSION_ID from cookie or Authorization header
     */
    private Mono<String> extractSessionId(ServerWebExchange exchange) {
        // Try cookie first
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME))
            .map(cookie -> cookie.getValue())
            .switchIfEmpty(
                // Try X-Session-Id header
                Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Session-Id"))
            );
    }

    // DTOs

    @Data
    public static class CreateSessionRequest {
        private String accessToken;
        private String refreshToken;
    }

    @Data
    public static class SessionResponse {
        private final String sessionId;
        private final String message;
    }

    @Data
    public static class LoginSuccessResponse {
        private final String sessionId;
        private final String userId;
        private final String username;
        private final String email;
        private final String message;
    }

    @Data
    public static class UserInfoResponse {
        private final String userId;
        private final String username;
        private final String email;
        private final boolean authenticated;
    }

    @Data
    public static class MessageResponse {
        private final String message;
    }
}
