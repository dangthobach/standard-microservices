package com.enterprise.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for API Gateway
 *
 * Features:
 * - OAuth2 Resource Server with JWT (validate JWT tokens)
 * - OAuth2 Login with PKCE Flow (redirect to Keycloak)
 * - Session Management with Redis
 * - CORS Configuration
 *
 * Dual Mode:
 * 1. OAuth2 Login: Traditional redirect-based flow for server-side apps
 * 2. Resource Server: Token validation for SPA (Frontend handles OAuth2)
 *
 * Flow Options:
 *
 * Option A (OAuth2 Login - Server-side):
 *   User -> /oauth2/authorization/keycloak
 *   -> Redirect to Keycloak
 *   -> Login at Keycloak
 *   -> Redirect back to /login/oauth2/code/keycloak
 *   -> Gateway creates session
 *   -> Redirect to /auth/login/success
 *
 * Option B (SPA - Client-side):
 *   Frontend handles OAuth2 PKCE
 *   -> Gets access token
 *   -> POST /auth/session with token
 *   -> Gateway creates session
 *   -> Returns SESSION_ID
 */
@Configuration
@EnableWebFluxSecurity
@org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
public class SecurityConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Main Security Filter Chain
     *
     * Supports both:
     * - OAuth2 Login (PKCE flow)
     * - OAuth2 Resource Server (JWT validation)
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http,
        ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        return http
            // Disable CSRF (we use stateless JWT + HttpOnly cookies)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization Rules
            .authorizeExchange(exchange -> exchange
                // Public endpoints
                .pathMatchers("/actuator/**", "/health/**").permitAll()
                .pathMatchers("/public/**").permitAll()

                // Auth endpoints (accessible without authentication)
                .pathMatchers("/auth/session", "/auth/login/**", "/auth/logout").permitAll()

                // OAuth2 endpoints (handled by Spring Security)
                .pathMatchers("/oauth2/**", "/login/**").permitAll()

                // Protected endpoints - require authentication
                .pathMatchers("/api/**").authenticated()
                .pathMatchers("/auth/me", "/auth/refresh").authenticated()

                // All other requests require authentication
                .anyExchange().authenticated()
            )

            // OAuth2 Login (PKCE Flow)
            .oauth2Login(oauth2Login -> oauth2Login
                // Default redirect after login
                .authenticationSuccessHandler((webFilterExchange, authentication) -> {
                    // Redirect to /auth/login/success to create session
                    webFilterExchange.getExchange().getResponse()
                        .getHeaders().setLocation(java.net.URI.create("/auth/login/success"));
                    webFilterExchange.getExchange().getResponse()
                        .setStatusCode(org.springframework.http.HttpStatus.FOUND);
                    return webFilterExchange.getExchange().getResponse().setComplete();
                })
            )

            // OAuth2 Resource Server (JWT Validation)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
            )

            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
            )

            .build();
    }

    /**
     * JWT Decoder for Resource Server mode
     *
     * Validates JWT signature using JWK from Keycloak
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * CORS Configuration
     *
     * Allow frontend to make cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins (use specific origins in production)
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Trace-Id",
            "X-Session-Id",
            "Set-Cookie"
        ));

        // Cache CORS preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * OIDC Logout Success Handler
     *
     * Redirects to Keycloak logout endpoint after local logout
     */
    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
        ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        OidcClientInitiatedServerLogoutSuccessHandler successHandler =
            new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);

        // Redirect to home after logout from Keycloak
        successHandler.setPostLogoutRedirectUri("{baseUrl}");

        return successHandler;
    }
}
