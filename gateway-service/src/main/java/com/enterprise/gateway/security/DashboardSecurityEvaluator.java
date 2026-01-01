package com.enterprise.gateway.security;

import com.enterprise.gateway.config.DashboardSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Dashboard Security Evaluator
 *
 * Evaluates whether a user has permission to access Dashboard APIs
 * based on dynamically configured roles.
 *
 * Logic: User needs ANY of the configured roles (OR logic)
 *
 * Usage in @PreAuthorize:
 * <pre>
 * @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
 * public Mono<ResponseEntity<?>> getDashboard() { ... }
 * </pre>
 *
 * Benefits over hardcoded roles:
 * - Configurable via application.yml (no code changes)
 * - Supports multiple roles easily
 * - Can be updated via Spring Cloud Config (runtime changes)
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Component("dashboardSecurity")
@RequiredArgsConstructor
public class DashboardSecurityEvaluator {

    private final DashboardSecurityProperties properties;

    /**
     * Check if user has access to Dashboard based on configured roles
     *
     * @param authentication Spring Security Authentication object
     * @return true if user has ANY of the allowed roles, false otherwise
     */
    public boolean hasAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Dashboard access denied: User not authenticated");
            return false;
        }

        // Get user's roles from authorities
        var userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role) // Remove ROLE_ prefix
                .toList();

        // Check if user has ANY of the allowed roles (OR logic)
        boolean hasAccess = properties.getAllowedRoles().stream()
                .anyMatch(allowedRole -> userRoles.contains(allowedRole));

        if (hasAccess) {
            log.debug("Dashboard access granted for user: {} with roles: {}",
                authentication.getName(), userRoles);
        } else {
            log.warn("Dashboard access denied for user: {} with roles: {}. Required: {}",
                authentication.getName(), userRoles, properties.getAllowedRoles());
        }

        return hasAccess;
    }
}
