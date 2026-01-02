package com.enterprise.iam.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for Keycloak Admin API
 * <p>
 * Provides access to Keycloak user management and token validation.
 * Uses circuit breaker, retry, and rate limiting from Resilience4j.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@FeignClient(
    name = "keycloak",
    url = "${app.keycloak.url:http://keycloak:8080}"
)
public interface KeycloakClient {

    /**
     * Get user details from Keycloak.
     *
     * @param realm  The Keycloak realm
     * @param userId The user ID
     * @return User details as JSON string
     */
    @GetMapping("/admin/realms/{realm}/users/{userId}")
    String getUser(@PathVariable("realm") String realm, @PathVariable("userId") String userId);

    /**
     * Get all users from Keycloak.
     *
     * @param realm The Keycloak realm
     * @return List of users as JSON string
     */
    @GetMapping("/admin/realms/{realm}/users")
    String getUsers(@PathVariable("realm") String realm);

    /**
     * Validate token by getting user info.
     *
     * @param realm The Keycloak realm
     * @return User info as JSON string
     */
    @GetMapping("/realms/{realm}/protocol/openid-connect/userinfo")
    String getUserInfo(@PathVariable("realm") String realm);
}
