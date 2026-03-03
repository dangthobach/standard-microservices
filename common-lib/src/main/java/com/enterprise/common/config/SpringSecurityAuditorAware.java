package com.enterprise.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(this::extractUsername)
            .or(() -> Optional.of("system")); // Fallback for background tasks
    }

    private String extractUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }

            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }

            return jwt.getSubject();
        }

        return authentication.getName();
    }
}
