package com.enterprise.iam.controller.internal;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.iam.entity.EndpointProtection;
import com.enterprise.iam.repository.EndpointProtectionRepository;
import com.enterprise.iam.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal AuthZ Controller
 * <p>
 * APIs used ONLY by Gateway-Service for Centralized Authorization.
 * Should be secured by Network Policy or Mutual TLS in production.
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "Internal AuthZ API", description = "Internal APIs for Gateway Synchronization")
public class InternalAuthZController {

    private final EndpointProtectionRepository endpointProtectionRepository;
    private final UserRepository userRepository;

    /**
     * Get all active Endpoint Policies.
     * Gateway calls this on startup and refresh.
     */
    @GetMapping("/policies")
    @Operation(summary = "Get active policies", description = "Returns list of active endpoint protection rules")
    public ResponseEntity<List<EndpointProtection>> getPolicies() {
        log.info("Gateway requested Policy Sync");
        return ResponseEntity.ok(
                endpointProtectionRepository.findByActiveTrueAndDeletedFalseOrderByPriorityDesc());
    }

    /**
     * Get Permission Codes for a User.
     * Gateway calls this on L2 Cache Miss.
     */
    @GetMapping("/permissions/user/{userId}")
    @Operation(summary = "Get user permissions", description = "Returns list of permission codes for a user")
    public ResponseEntity<List<String>> getUserPermissions(@PathVariable UUID userId) {
        log.debug("Gateway requested Permissions for User: {}", userId);
        return ResponseEntity.ok(
                userRepository.findPermissionCodesByUserId(userId));
    }
}
