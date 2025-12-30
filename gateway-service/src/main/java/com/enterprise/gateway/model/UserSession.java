package com.enterprise.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * User Session Model
 *
 * Stored in Redis with SESSION_ID as key
 * Contains JWT tokens and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    /**
     * Session ID (UUID)
     */
    private String sessionId;

    /**
     * User ID from Keycloak
     */
    private String userId;

    /**
     * Username (preferred_username from JWT)
     */
    private String username;

    /**
     * Email
     */
    private String email;

    /**
     * Access Token (JWT)
     */
    private String accessToken;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * Access Token Expiration Time
     */
    private Instant accessTokenExpiresAt;

    /**
     * Refresh Token Expiration Time
     */
    private Instant refreshTokenExpiresAt;

    /**
     * Session Creation Time
     */
    private Instant createdAt;

    /**
     * Last Access Time
     */
    private Instant lastAccessedAt;

    /**
     * Check if access token is expired
     */
    public boolean isAccessTokenExpired() {
        return accessTokenExpiresAt != null &&
               Instant.now().isAfter(accessTokenExpiresAt);
    }

    /**
     * Check if refresh token is expired
     */
    public boolean isRefreshTokenExpired() {
        return refreshTokenExpiresAt != null &&
               Instant.now().isAfter(refreshTokenExpiresAt);
    }

    /**
     * Check if session is valid
     */
    public boolean isValid() {
        return !isRefreshTokenExpired();
    }

    /**
     * Update last accessed time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }
}
