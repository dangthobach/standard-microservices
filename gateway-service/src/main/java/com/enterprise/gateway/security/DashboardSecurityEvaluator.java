package com.enterprise.gateway.security;

import com.enterprise.gateway.config.DashboardSecurityProperties;
import com.enterprise.gateway.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Dashboard Security Evaluator
 *
 * Evaluates whether a user has permission to access Dashboard APIs
 * based on roles stored in IAM database.
 *
 * Architecture Note:
 * - JWT tokens do NOT contain role claims in this architecture
 * - Roles are managed in IAM database (users -> user_roles -> roles)
 * - This evaluator fetches roles from IAM via UserRoleService (with L1/L2 caching)
 *
 * Logic: User needs ANY of the configured roles (OR logic)
 *
 * Usage in @PreAuthorize (for reactive methods):
 * <pre>
 * @PreAuthorize("@dashboardSecurity.hasAccess(authentication).block()")
 * public Mono<ResponseEntity<?>> getDashboard() { ... }
 * </pre>
 *
 * Or for WebFlux-native authorization:
 * <pre>
 * dashboardSecurity.hasAccess(authentication)
 *     .flatMap(hasAccess -> hasAccess ? proceed() : forbidden())
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Component("dashboardSecurity")
@RequiredArgsConstructor
public class DashboardSecurityEvaluator {

    private final DashboardSecurityProperties properties;
    private final UserRoleService userRoleService;

    /**
     * Check if user has access to Dashboard based on roles from IAM database.
     * <p>
     * Flow:
     * 1. Extract keycloakId (sub claim) from JWT
     * 2. Fetch user roles from IAM (via UserRoleService with caching)
     * 3. Check if user has ANY of the allowed roles
     *
     * @param authentication Spring Security Authentication object
     * @return Mono<Boolean> true if user has ANY of the allowed roles
     */
    public Mono<Boolean> hasAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Dashboard access denied: User not authenticated");
            return Mono.just(false);
        }

        // Extract keycloakId from JWT 'sub' claim
        String keycloakId = extractKeycloakId(authentication);
        if (keycloakId == null) {
            log.warn("Dashboard access denied: Could not extract keycloakId from authentication");
            return Mono.just(false);
        }

        // Fetch roles from IAM and check against allowed roles
        return userRoleService.hasAnyRole(keycloakId, properties.getAllowedRoles())
                .doOnNext(hasAccess -> {
                    if (hasAccess) {
                        log.debug("Dashboard access granted for keycloakId: {}", keycloakId);
                    } else {
                        log.warn("Dashboard access denied for keycloakId: {}. Required roles: {}",
                                keycloakId, properties.getAllowedRoles());
                    }
                });
    }

    /**
     * Synchronous version for use with @PreAuthorize.
     * Note: This blocks and should be used carefully in reactive context.
     *
     * @param authentication Spring Security Authentication object
     * @return true if user has ANY of the allowed roles
     */
    public boolean hasAccessSync(Authentication authentication) {
        return Boolean.TRUE.equals(hasAccess(authentication).block());
    }

    /**
     * Extract Keycloak ID from authentication.
     * The 'sub' claim in JWT contains the Keycloak user ID.
     */
    private String extractKeycloakId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("sub");
        }
        
        // Fallback: try to get from principal name
        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        
        return null;
    }
}

