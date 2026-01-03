package com.enterprise.iam.service;

import com.enterprise.iam.entity.*;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRequestHistoryRepository;
import com.enterprise.iam.repository.UserRequestRepository;
import com.enterprise.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Request Service
 * <p>
 * Implements Maker/Checker workflow for user creation requests.
 * <p>
 * Features:
 * - State machine validation (DRAFT -> WAITING_FOR_APPROVAL -> APPROVED/REJECTED)
 * - Separation of duties (Maker cannot approve own request)
 * - Audit logging via UserRequestHistory
 * - Automatic user creation on approval
 * - Event publishing for email notifications
 * <p>
 * Permissions Required:
 * - USER_REQUEST_CREATE: Create/update requests (Maker)
 * - USER_REQUEST_VIEW: View requests (Both)
 * - USER_REQUEST_APPROVE: Approve/reject requests (Checker)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRequestService {

    private final UserRequestRepository userRequestRepository;
    private final UserRequestHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new user request (DRAFT state)
     *
     * @param email       Email for new user
     * @param firstName   First name
     * @param lastName    Last name
     * @param roleIds     Set of role IDs to assign
     * @return Created UserRequest
     */
    @Transactional
    public UserRequest createRequest(String email, String firstName, String lastName, Set<UUID> roleIds) {
        log.info("Creating user request: email={}, creator={}", email, getCurrentUserId());

        // Validate email uniqueness
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new IllegalArgumentException("User with email '" + email + "' already exists");
        }

        if (userRequestRepository.existsByEmailAndDeletedFalse(email)) {
            throw new IllegalArgumentException("Request with email '" + email + "' already exists");
        }

        // Validate roles exist
        validateRoles(roleIds);

        // Create request
        UserRequest request = UserRequest.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roleIds(roleIds != null ? roleIds : Set.of())
                .requestCreatorId(getCurrentUserId())
                .build();

        // Initialize status to DRAFT
        request.setStatus(UserRequestStatus.DRAFT);

        // Save request
        request = userRequestRepository.save(request);

        // Create history entry
        createHistoryEntry(request, null, UserRequestStatus.DRAFT, UserRequestAction.CREATE, "Request created");

        log.info("✅ User request created: id={}, email={}", request.getId(), request.getEmail());
        return request;
    }

    /**
     * Update a DRAFT or REJECTED request
     *
     * @param requestId   Request ID
     * @param email       Updated email
     * @param firstName   Updated first name
     * @param lastName    Updated last name
     * @param roleIds     Updated role IDs
     * @return Updated UserRequest
     */
    @Transactional
    public UserRequest updateRequest(UUID requestId, String email, String firstName, String lastName, Set<UUID> roleIds) {
        log.info("Updating user request: id={}, updater={}", requestId, getCurrentUserId());

        UserRequest request = userRequestRepository.findByIdAndNotDeleted(requestId)
                .orElseThrow(() -> new IllegalArgumentException("User request not found: " + requestId));

        // Only DRAFT or REJECTED requests can be updated
        if (!request.hasAnyStatus(UserRequestStatus.DRAFT, UserRequestStatus.REJECTED)) {
            throw new IllegalStateException("Cannot update request in status: " + request.getStatus());
        }

        // Only creator can update
        if (!request.getRequestCreatorId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("Only the request creator can update this request");
        }

        // Validate email uniqueness (if changed)
        if (!request.getEmail().equals(email)) {
            if (userRepository.existsByEmailAndDeletedFalse(email)) {
                throw new IllegalArgumentException("User with email '" + email + "' already exists");
            }
            if (userRequestRepository.existsByEmailAndDeletedFalse(email)) {
                throw new IllegalArgumentException("Request with email '" + email + "' already exists");
            }
        }

        // Validate roles
        validateRoles(roleIds);

        // Update fields
        UserRequestStatus oldStatus = request.getStatus();
        request.setEmail(email);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setRoleIds(roleIds != null ? roleIds : Set.of());

        // If REJECTED, keep status (will be resubmitted later)
        // If DRAFT, keep as DRAFT

        request = userRequestRepository.save(request);

        // Create history entry
        createHistoryEntry(request, oldStatus, request.getStatus(), UserRequestAction.UPDATE, "Request updated");

        log.info("✅ User request updated: id={}", requestId);
        return request;
    }

    /**
     * Submit request for approval (DRAFT -> WAITING_FOR_APPROVAL)
     *
     * @param requestId Request ID
     * @return Updated UserRequest
     */
    @Transactional
    public UserRequest submitRequest(UUID requestId) {
        log.info("Submitting user request: id={}, submitter={}", requestId, getCurrentUserId());

        UserRequest request = userRequestRepository.findByIdAndNotDeleted(requestId)
                .orElseThrow(() -> new IllegalArgumentException("User request not found: " + requestId));

        // Only DRAFT requests can be submitted
        if (!request.hasStatus(UserRequestStatus.DRAFT)) {
            throw new IllegalStateException("Only DRAFT requests can be submitted. Current status: " + request.getStatus());
        }

        // Only creator can submit
        if (!request.getRequestCreatorId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("Only the request creator can submit this request");
        }

        // Transition to WAITING_FOR_APPROVAL
        UserRequestStatus oldStatus = request.getStatus();
        request.changeStatus(UserRequestStatus.WAITING_FOR_APPROVAL, getCurrentUserId(), "Request submitted for approval");
        request = userRequestRepository.save(request);

        // Create history entry
        createHistoryEntry(request, oldStatus, UserRequestStatus.WAITING_FOR_APPROVAL, UserRequestAction.SUBMIT, "Request submitted");

        log.info("✅ User request submitted: id={}", requestId);
        return request;
    }

    /**
     * Approve a request (WAITING_FOR_APPROVAL -> APPROVED)
     * <p>
     * Separation of Duties: Checker cannot be the same as Maker
     *
     * @param requestId Request ID
     * @return Updated UserRequest
     */
    @Transactional
    public UserRequest approveRequest(UUID requestId) {
        log.info("Approving user request: id={}, approver={}", requestId, getCurrentUserId());

        UserRequest request = userRequestRepository.findByIdAndNotDeleted(requestId)
                .orElseThrow(() -> new IllegalArgumentException("User request not found: " + requestId));

        // Only WAITING_FOR_APPROVAL requests can be approved
        if (!request.hasStatus(UserRequestStatus.WAITING_FOR_APPROVAL)) {
            throw new IllegalStateException("Only WAITING_FOR_APPROVAL requests can be approved. Current status: " + request.getStatus());
        }

        // SEPARATION OF DUTIES: Checker cannot be the same as Maker
        String checkerId = getCurrentUserId();
        if (request.getRequestCreatorId().equals(checkerId)) {
            throw new AccessDeniedException(
                    "Separation of duties violation: Request creator cannot approve their own request. " +
                            "Creator: " + request.getRequestCreatorId() + ", Checker: " + checkerId
            );
        }

        // Transition to APPROVED
        UserRequestStatus oldStatus = request.getStatus();
        request.changeStatus(UserRequestStatus.APPROVED, checkerId, "Request approved");
        request = userRequestRepository.save(request);

        // Create history entry
        createHistoryEntry(request, oldStatus, UserRequestStatus.APPROVED, UserRequestAction.APPROVE, "Request approved");

        // Create actual User entity
        createUserFromRequest(request);

        // Publish event for email notification
        publishUserRequestProcessedEvent(request, true, null);

        log.info("✅ User request approved: id={}, user created", requestId);
        return request;
    }

    /**
     * Reject a request (WAITING_FOR_APPROVAL -> REJECTED)
     * <p>
     * Separation of Duties: Checker cannot be the same as Maker
     * MANDATORY: rejectReason must be non-empty
     *
     * @param requestId    Request ID
     * @param rejectReason Reason for rejection (MANDATORY)
     * @return Updated UserRequest
     */
    @Transactional
    public UserRequest rejectRequest(UUID requestId, String rejectReason) {
        log.info("Rejecting user request: id={}, rejecter={}", requestId, getCurrentUserId());

        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reject reason is mandatory");
        }

        UserRequest request = userRequestRepository.findByIdAndNotDeleted(requestId)
                .orElseThrow(() -> new IllegalArgumentException("User request not found: " + requestId));

        // Only WAITING_FOR_APPROVAL requests can be rejected
        if (!request.hasStatus(UserRequestStatus.WAITING_FOR_APPROVAL)) {
            throw new IllegalStateException("Only WAITING_FOR_APPROVAL requests can be rejected. Current status: " + request.getStatus());
        }

        // SEPARATION OF DUTIES: Checker cannot be the same as Maker
        String checkerId = getCurrentUserId();
        if (request.getRequestCreatorId().equals(checkerId)) {
            throw new AccessDeniedException(
                    "Separation of duties violation: Request creator cannot reject their own request. " +
                            "Creator: " + request.getRequestCreatorId() + ", Checker: " + checkerId
            );
        }

        // Transition to REJECTED
        UserRequestStatus oldStatus = request.getStatus();
        request.changeStatus(UserRequestStatus.REJECTED, checkerId, rejectReason);
        request = userRequestRepository.save(request);

        // Create history entry
        createHistoryEntry(request, oldStatus, UserRequestStatus.REJECTED, UserRequestAction.REJECT, rejectReason);

        // Publish event for email notification
        publishUserRequestProcessedEvent(request, false, rejectReason);

        log.info("✅ User request rejected: id={}, reason={}", requestId, rejectReason);
        return request;
    }

    /**
     * Batch approve multiple requests
     *
     * @param requestIds List of request IDs to approve
     * @return List of approved requests
     */
    @Transactional
    public List<UserRequest> batchApprove(List<UUID> requestIds) {
        log.info("Batch approving {} requests: approver={}", requestIds.size(), getCurrentUserId());

        return requestIds.stream()
                .map(this::approveRequest)
                .collect(Collectors.toList());
    }

    /**
     * Batch reject multiple requests
     *
     * @param requestIds   List of request IDs to reject
     * @param rejectReason Reason for rejection (MANDATORY)
     * @return List of rejected requests
     */
    @Transactional
    public List<UserRequest> batchReject(List<UUID> requestIds, String rejectReason) {
        log.info("Batch rejecting {} requests: rejecter={}", requestIds.size(), getCurrentUserId());

        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reject reason is mandatory");
        }

        return requestIds.stream()
                .map(id -> rejectRequest(id, rejectReason))
                .collect(Collectors.toList());
    }

    /**
     * Get request by ID
     */
    @Transactional(readOnly = true)
    public UserRequest getRequest(UUID requestId) {
        return userRequestRepository.findByIdAndNotDeleted(requestId)
                .orElseThrow(() -> new IllegalArgumentException("User request not found: " + requestId));
    }

    /**
     * Get all requests with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserRequest> getAllRequests(Pageable pageable) {
        return userRequestRepository.findAll(pageable);
    }

    /**
     * Get requests by status
     */
    @Transactional(readOnly = true)
    public Page<UserRequest> getRequestsByStatus(UserRequestStatus status, Pageable pageable) {
        return userRequestRepository.findByStatusAndDeletedFalse(status, pageable);
    }

    /**
     * Get requests by creator
     */
    @Transactional(readOnly = true)
    public Page<UserRequest> getRequestsByCreator(String creatorId, Pageable pageable) {
        return userRequestRepository.findByRequestCreatorIdAndDeletedFalse(creatorId, pageable);
    }

    /**
     * Get request history
     */
    @Transactional(readOnly = true)
    public List<UserRequestHistory> getRequestHistory(UUID requestId) {
        return historyRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }

    /**
     * Create User entity from approved request
     */
    private void createUserFromRequest(UserRequest request) {
        log.debug("Creating user from approved request: requestId={}, email={}", request.getId(), request.getEmail());

        // Load roles
        Set<com.enterprise.iam.entity.Role> roles = request.getRoleIds().stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId)))
                .collect(Collectors.toSet());

        // Create user (Note: keycloakId will be set later when Keycloak user is created)
        // For now, we use a placeholder or generate one
        String keycloakId = "pending-" + request.getId().toString();

        User user = User.builder()
                .keycloakId(keycloakId)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .emailVerified(false)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        log.info("✅ User created from request: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * Create history entry for audit logging
     */
    private void createHistoryEntry(UserRequest request, UserRequestStatus oldStatus, UserRequestStatus newStatus,
                                    UserRequestAction action, String comment) {
        UserRequestHistory history = UserRequestHistory.builder()
                .request(request)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .action(action)
                .actorId(getCurrentUserId())
                .comment(comment)
                .build();

        historyRepository.save(history);
    }

    /**
     * Publish UserRequestProcessedEvent for email notifications
     */
    private void publishUserRequestProcessedEvent(UserRequest request, boolean approved, String rejectReason) {
        UserRequestProcessedEvent event = new UserRequestProcessedEvent(
                request.getId(),
                request.getEmail(),
                request.getFullName(),
                request.getStatus(),
                approved,
                rejectReason
        );

        eventPublisher.publishEvent(event);
        log.debug("📢 Published UserRequestProcessedEvent: requestId={}, approved={}", request.getId(), approved);
    }

    /**
     * Validate that all role IDs exist
     */
    private void validateRoles(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        for (UUID roleId : roleIds) {
            if (!roleRepository.existsById(roleId)) {
                throw new IllegalArgumentException("Role not found: " + roleId);
            }
        }
    }

    /**
     * Get current user ID from SecurityContext
     * Extracts from JWT token (preferred_username or email)
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            // Try preferred_username (Keycloak standard)
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }

            // Try email
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }

            // Fallback to subject
            return jwt.getSubject();
        }

        return authentication.getName();
    }

    /**
     * Domain Event: User Request Processed
     * <p>
     * Published when a request is approved or rejected.
     * Used for email notifications.
     */
    public record UserRequestProcessedEvent(
            UUID requestId,
            String email,
            String fullName,
            UserRequestStatus status,
            boolean approved,
            String rejectReason
    ) {
    }
}

