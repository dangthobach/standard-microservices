package com.enterprise.iam.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.iam.dto.*;
import com.enterprise.iam.entity.UserRequest;
import com.enterprise.iam.entity.UserRequestHistory;
import com.enterprise.iam.entity.UserRequestStatus;
import com.enterprise.iam.service.UserRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Request Controller
 * <p>
 * REST API for Maker/Checker workflow for user creation requests.
 * <p>
 * Endpoints:
 * - POST /api/iam/requests - Create draft request
 * - PUT /api/iam/requests/{id} - Update draft/rejected request
 * - POST /api/iam/requests/{id}/submit - Submit for approval
 * - POST /api/iam/requests/{id}/approve - Approve request (Checker only)
 * - POST /api/iam/requests/{id}/reject - Reject request (Checker only)
 * - POST /api/iam/requests/batch/approve - Batch approve
 * - POST /api/iam/requests/batch/reject - Batch reject
 * - GET /api/iam/requests - List all requests (paginated)
 * - GET /api/iam/requests/{id} - Get request by ID
 * - GET /api/iam/requests/{id}/history - Get request history
 */
@Slf4j
@RestController
@RequestMapping("/api/iam/requests")
@RequiredArgsConstructor
@Tag(name = "User Request Management", description = "Maker/Checker workflow for user creation requests")
public class UserRequestController {

    private final UserRequestService userRequestService;

    /**
     * Create a new user request (DRAFT)
     * <p>
     * Requires: USER_REQUEST_CREATE permission
     */
    @PostMapping
    @Operation(summary = "Create user request", description = "Create a new user creation request in DRAFT status")
    public ResponseEntity<ApiResponse<UserRequestDTO>> createRequest(
            @Valid @RequestBody CreateUserRequestDTO dto
    ) {
        log.info("Creating user request: email={}", dto.getEmail());

        UserRequest request = userRequestService.createRequest(
                dto.getEmail(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getRoleIds()
        );

        return ResponseEntity.ok(
                ApiResponse.success("User request created successfully", convertToDTO(request))
        );
    }

    /**
     * Update a DRAFT or REJECTED request
     * <p>
     * Requires: USER_REQUEST_CREATE permission
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user request", description = "Update a DRAFT or REJECTED user request")
    public ResponseEntity<ApiResponse<UserRequestDTO>> updateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequestDTO dto
    ) {
        log.info("Updating user request: id={}", id);

        UserRequest request = userRequestService.updateRequest(
                id,
                dto.getEmail(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getRoleIds()
        );

        return ResponseEntity.ok(
                ApiResponse.success("User request updated successfully", convertToDTO(request))
        );
    }

    /**
     * Submit request for approval (DRAFT -> WAITING_FOR_APPROVAL)
     * <p>
     * Requires: USER_REQUEST_CREATE permission
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit request for approval", description = "Submit a DRAFT request for approval")
    public ResponseEntity<ApiResponse<UserRequestDTO>> submitRequest(@PathVariable UUID id) {
        log.info("Submitting user request: id={}", id);

        UserRequest request = userRequestService.submitRequest(id);

        return ResponseEntity.ok(
                ApiResponse.success("User request submitted for approval", convertToDTO(request))
        );
    }

    /**
     * Approve a request (WAITING_FOR_APPROVAL -> APPROVED)
     * <p>
     * Requires: USER_REQUEST_APPROVE permission
     * Separation of Duties: Checker cannot be the same as Maker
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve user request", description = "Approve a WAITING_FOR_APPROVAL request (Checker only)")
    public ResponseEntity<ApiResponse<UserRequestDTO>> approveRequest(@PathVariable UUID id) {
        log.info("Approving user request: id={}", id);

        UserRequest request = userRequestService.approveRequest(id);

        return ResponseEntity.ok(
                ApiResponse.success("User request approved and user created", convertToDTO(request))
        );
    }

    /**
     * Reject a request (WAITING_FOR_APPROVAL -> REJECTED)
     * <p>
     * Requires: USER_REQUEST_APPROVE permission
     * Separation of Duties: Checker cannot be the same as Maker
     * MANDATORY: rejectReason must be provided
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject user request", description = "Reject a WAITING_FOR_APPROVAL request (Checker only, reason required)")
    public ResponseEntity<ApiResponse<UserRequestDTO>> rejectRequest(
            @PathVariable UUID id,
            @RequestBody RejectRequestDTO dto
    ) {
        log.info("Rejecting user request: id={}, reason={}", id, dto.getReason());

        UserRequest request = userRequestService.rejectRequest(id, dto.getReason());

        return ResponseEntity.ok(
                ApiResponse.success("User request rejected", convertToDTO(request))
        );
    }

    /**
     * Batch approve multiple requests
     * <p>
     * Requires: USER_REQUEST_APPROVE permission
     */
    @PostMapping("/batch/approve")
    @Operation(summary = "Batch approve requests", description = "Approve multiple requests at once")
    public ResponseEntity<ApiResponse<List<UserRequestDTO>>> batchApprove(
            @Valid @RequestBody BatchApproveDTO dto
    ) {
        log.info("Batch approving {} requests", dto.getRequestIds().size());

        List<UserRequest> requests = userRequestService.batchApprove(dto.getRequestIds());

        List<UserRequestDTO> dtos = requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Requests approved successfully", dtos)
        );
    }

    /**
     * Batch reject multiple requests
     * <p>
     * Requires: USER_REQUEST_APPROVE permission
     */
    @PostMapping("/batch/reject")
    @Operation(summary = "Batch reject requests", description = "Reject multiple requests at once (reason required)")
    public ResponseEntity<ApiResponse<List<UserRequestDTO>>> batchReject(
            @Valid @RequestBody BatchRejectDTO dto
    ) {
        log.info("Batch rejecting {} requests", dto.getRequestIds().size());

        List<UserRequest> requests = userRequestService.batchReject(dto.getRequestIds(), dto.getReason());

        List<UserRequestDTO> dtos = requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Requests rejected successfully", dtos)
        );
    }

    /**
     * Get all requests with pagination
     * <p>
     * Requires: USER_REQUEST_VIEW permission
     */
    @GetMapping
    @Operation(summary = "Get all requests", description = "Retrieve paginated list of all user requests")
    public ResponseEntity<ApiResponse<PageResponse<UserRequestDTO>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserRequestStatus status
    ) {
        log.info("Fetching requests: page={}, size={}, status={}", page, size, status);

        Pageable pageable = PageRequest.of(page, size);
        Page<UserRequest> requestPage;

        if (status != null) {
            requestPage = userRequestService.getRequestsByStatus(status, pageable);
        } else {
            requestPage = userRequestService.getAllRequests(pageable);
        }

        List<UserRequestDTO> dtos = requestPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResponse<UserRequestDTO> pageResponse = PageResponse.from(requestPage, dtos);

        return ResponseEntity.ok(
                ApiResponse.success("Requests retrieved successfully", pageResponse)
        );
    }

    /**
     * Get request by ID
     * <p>
     * Requires: USER_REQUEST_VIEW permission
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get request by ID", description = "Retrieve user request details by ID")
    public ResponseEntity<ApiResponse<UserRequestDTO>> getRequest(@PathVariable UUID id) {
        log.info("Fetching user request: id={}", id);

        UserRequest request = userRequestService.getRequest(id);

        return ResponseEntity.ok(
                ApiResponse.success("Request retrieved successfully", convertToDTO(request))
        );
    }

    /**
     * Get request history (audit log)
     * <p>
     * Requires: USER_REQUEST_VIEW permission
     */
    @GetMapping("/{id}/history")
    @Operation(summary = "Get request history", description = "Retrieve audit log for a user request")
    public ResponseEntity<ApiResponse<List<UserRequestHistoryDTO>>> getRequestHistory(@PathVariable UUID id) {
        log.info("Fetching request history: id={}", id);

        List<UserRequestHistory> history = userRequestService.getRequestHistory(id);

        List<UserRequestHistoryDTO> dtos = history.stream()
                .map(this::convertHistoryToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Request history retrieved successfully", dtos)
        );
    }

    /**
     * Convert UserRequest to DTO
     */
    private UserRequestDTO convertToDTO(UserRequest request) {
        return UserRequestDTO.builder()
                .id(request.getId())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .fullName(request.getFullName())
                .roleIds(request.getRoleIds())
                .status(request.getStatus())
                .requestCreatorId(request.getRequestCreatorId())
                .statusChangeReason(request.getStatusChangeReason())
                .statusChangedAt(request.getStatusChangedAt())
                .statusChangedBy(request.getStatusChangedBy())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    /**
     * Convert UserRequestHistory to DTO
     */
    private UserRequestHistoryDTO convertHistoryToDTO(UserRequestHistory history) {
        return UserRequestHistoryDTO.builder()
                .id(history.getId())
                .requestId(history.getRequest().getId())
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .action(history.getAction())
                .actorId(history.getActorId())
                .comment(history.getComment())
                .metadata(history.getMetadata())
                .createdAt(history.getCreatedAt())
                .build();
    }

    /**
     * Reject Request DTO (for single reject endpoint)
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RejectRequestDTO {
        private String reason;
    }
}

