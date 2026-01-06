package com.enterprise.business.client;

import com.enterprise.business.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for IAM Service
 * <p>
 * Example of internal microservice-to-microservice communication
 * using JWT forwarding authentication.
 * <p>
 * Configuration:
 * - Uses FeignClientConfiguration for standard timeouts and error handling
 * - Uses JwtForwardingInterceptor to propagate JWT from incoming request
 * - Service discovery via Consul (LoadBalancer resolves service name)
 * <p>
 * Service Discovery:
 * - Service name "iam-service" is resolved via Consul
 * - LoadBalancer automatically distributes requests across multiple instances
 * - No hardcoded URL needed - fully dynamic service discovery
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@FeignClient(
    name = "iam-service",  // Service name resolved via Consul
    // url attribute removed - using service discovery instead
    configuration = IamServiceFeignConfiguration.class
)
public interface IamServiceClient {

    /**
     * Get user by ID from IAM service.
     *
     * @param userId User ID
     * @return User details
     */
    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId);

    /**
     * Verify user permissions.
     *
     * @param userId User ID
     * @param permission Permission to check
     * @return true if user has permission
     */
    @GetMapping("/api/users/{userId}/permissions/{permission}")
    Boolean hasPermission(
        @PathVariable("userId") String userId,
        @PathVariable("permission") String permission
    );
}
