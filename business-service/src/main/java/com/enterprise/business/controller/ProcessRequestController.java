package com.enterprise.business.controller;

import com.enterprise.business.service.RequestProducer;
import com.enterprise.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Process Request Controller
 * <p>
 * Allows triggering workflow processes manually for testing and ad-hoc scenarios.
 * In production, processes are typically triggered automatically by business events
 * (e.g., product creation, order submission).
 * <p>
 * This controller demonstrates:
 * - Sending process requests to Process Management Service via RabbitMQ
 * - Using Protobuf for efficient message serialization
 * - Decoupled async communication between services
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/process-requests")
@RequiredArgsConstructor
@Tag(name = "Process Request Management", description = "Trigger and manage workflow processes")
public class ProcessRequestController {

    private final RequestProducer requestProducer;

    /**
     * Create a new process request
     * <p>
     * Example: POST /api/process-requests
     * <pre>
     * {
     *   "processDefinitionKey": "product-approval-process",
     *   "initiatorUserId": "user123",
     *   "businessKey": "PROD-001",
     *   "variables": {
     *     "productId": "550e8400-e29b-41d4-a716-446655440000",
     *     "productName": "Laptop Pro 15",
     *     "price": "1299.99"
     *   },
     *   "priority": 5
     * }
     * </pre>
     *
     * @param request Process request payload
     * @return Request ID for tracking
     */
    @PostMapping
    @PreAuthorize("hasAuthority('process:create')")
    @Operation(
        summary = "Create process request",
        description = "Trigger a new workflow process by sending a request to Process Management Service"
    )
    public ResponseEntity<ApiResponse<UUID>> createProcessRequest(@RequestBody ProcessRequestDTO request) {
        log.info("REST request to create process: key={}, businessKey={}", 
            request.getProcessDefinitionKey(), request.getBusinessKey());

        UUID requestId = requestProducer.sendProcessRequest(
            request.getProcessDefinitionKey(),
            request.getInitiatorUserId(),
            request.getBusinessKey(),
            request.getVariables(),
            request.getPriority() != null ? request.getPriority() : 5
        );

        log.info("Process request sent successfully: requestId={}", requestId);

        return ResponseEntity.ok(
            ApiResponse.success("Process request created and sent to Process Management Service", requestId)
        );
    }

    /**
     * DTO for Process Request creation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProcessRequestDTO {
        private String processDefinitionKey;
        private String initiatorUserId;
        private String businessKey;
        private Map<String, Object> variables;
        private Integer priority;
    }
}
