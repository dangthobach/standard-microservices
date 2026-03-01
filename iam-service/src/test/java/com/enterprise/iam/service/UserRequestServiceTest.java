package com.enterprise.iam.service;

import com.enterprise.iam.entity.*;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRequestHistoryRepository;
import com.enterprise.iam.repository.UserRequestRepository;
import com.enterprise.iam.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserRequestService.
 * Tests the Maker/Checker workflow, state transitions, separation of duties,
 * and audit logging.
 */
@ExtendWith(MockitoExtension.class)
class UserRequestServiceTest {

    @Mock
    private UserRequestRepository userRequestRepository;
    @Mock
    private UserRequestHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserRequestService userRequestService;

    private static final String MAKER_ID = "maker@example.com";
    private static final String CHECKER_ID = "checker@example.com";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Sets up SecurityContext with a real JWT for the given userId
     */
    private void setCurrentUser(String userId) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("preferred_username", userId)
                .claim("sub", userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Creates a UserRequest in the given status for testing
     */
    private UserRequest createRequest(UUID id, UserRequestStatus status, String creatorId) {
        UserRequest request = UserRequest.builder()
                .email("newuser@example.com")
                .firstName("New")
                .lastName("User")
                .roleIds(Set.of())
                .requestCreatorId(creatorId)
                .build();
        request.setId(id);
        request.setStatus(status);
        return request;
    }

    // -------------------------------------------------------------------
    // createRequest()
    // -------------------------------------------------------------------
    @Nested
    @DisplayName("createRequest()")
    class CreateRequestTests {

        @Test
        @DisplayName("should create a DRAFT request successfully")
        void shouldCreateDraftRequest() {
            setCurrentUser(MAKER_ID);
            when(userRepository.existsByEmailAndDeletedFalse("newuser@example.com")).thenReturn(false);
            when(userRequestRepository.existsByEmailAndDeletedFalse("newuser@example.com")).thenReturn(false);
            when(userRequestRepository.save(any(UserRequest.class))).thenAnswer(inv -> {
                var req = inv.getArgument(0, UserRequest.class);
                req.setId(UUID.randomUUID());
                return req;
            });
            when(historyRepository.save(any(UserRequestHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = userRequestService.createRequest(
                    "newuser@example.com", "New", "User", Set.of());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(UserRequestStatus.DRAFT);
            assertThat(result.getEmail()).isEqualTo("newuser@example.com");
            assertThat(result.getRequestCreatorId()).isEqualTo(MAKER_ID);

            verify(userRequestRepository).save(any(UserRequest.class));
            verify(historyRepository).save(any(UserRequestHistory.class));
        }

        @Test
        @DisplayName("should throw when email already exists as user")
        void shouldThrowWhenEmailExistsAsUser() {
            setCurrentUser(MAKER_ID);
            when(userRepository.existsByEmailAndDeletedFalse("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userRequestService.createRequest("existing@example.com", "A", "B", Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw when email already exists as pending request")
        void shouldThrowWhenEmailExistsAsRequest() {
            setCurrentUser(MAKER_ID);
            when(userRepository.existsByEmailAndDeletedFalse("pending@example.com")).thenReturn(false);
            when(userRequestRepository.existsByEmailAndDeletedFalse("pending@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userRequestService.createRequest("pending@example.com", "A", "B", Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should validate role IDs exist")
        void shouldValidateRoleIds() {
            setCurrentUser(MAKER_ID);
            var roleId = UUID.randomUUID();
            when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
            when(userRequestRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
            when(roleRepository.existsById(roleId)).thenReturn(false);

            assertThatThrownBy(() -> userRequestService.createRequest("new@example.com", "A", "B", Set.of(roleId)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    // -------------------------------------------------------------------
    // submitRequest()
    // -------------------------------------------------------------------
    @Nested
    @DisplayName("submitRequest()")
    class SubmitRequestTests {

        @Test
        @DisplayName("should transition DRAFT -> WAITING_FOR_APPROVAL")
        void shouldTransitionDraftToWaiting() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.DRAFT, MAKER_ID);
            setCurrentUser(MAKER_ID);
            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));
            when(userRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = userRequestService.submitRequest(requestId);

            assertThat(result.getStatus()).isEqualTo(UserRequestStatus.WAITING_FOR_APPROVAL);
            verify(historyRepository).save(any(UserRequestHistory.class));
        }

        @Test
        @DisplayName("should throw when submitting non-DRAFT request")
        void shouldThrowWhenNotDraft() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.APPROVED, MAKER_ID);
            setCurrentUser(MAKER_ID);
            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> userRequestService.submitRequest(requestId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only DRAFT");
        }

        @Test
        @DisplayName("should throw when non-creator tries to submit")
        void shouldThrowWhenNonCreator() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.DRAFT, MAKER_ID);
            setCurrentUser(CHECKER_ID);
            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> userRequestService.submitRequest(requestId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only the request creator");
        }
    }

    // -------------------------------------------------------------------
    // approveRequest()
    // -------------------------------------------------------------------
    @Nested
    @DisplayName("approveRequest()")
    class ApproveRequestTests {

        @Test
        @DisplayName("should approve and create user")
        void shouldApproveAndCreateUser() {
            var requestId = UUID.randomUUID();
            var roleId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.WAITING_FOR_APPROVAL, MAKER_ID);
            request.setRoleIds(Set.of(roleId));
            setCurrentUser(CHECKER_ID);

            var role = new Role();
            role.setId(roleId);

            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));
            when(userRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                var user = inv.getArgument(0, User.class);
                user.setId(UUID.randomUUID());
                return user;
            });

            var result = userRequestService.approveRequest(requestId);

            assertThat(result.getStatus()).isEqualTo(UserRequestStatus.APPROVED);
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(UserRequestService.UserRequestProcessedEvent.class));
        }

        @Test
        @DisplayName("should enforce separation of duties — creator cannot approve")
        void shouldEnforceSeparationOfDuties() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.WAITING_FOR_APPROVAL, MAKER_ID);
            setCurrentUser(MAKER_ID); // Same as creator

            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> userRequestService.approveRequest(requestId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Separation of duties");
        }

        @Test
        @DisplayName("should throw when approving non-WAITING request")
        void shouldThrowWhenNotWaiting() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.DRAFT, MAKER_ID);
            setCurrentUser(CHECKER_ID);
            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> userRequestService.approveRequest(requestId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------
    // rejectRequest()
    // -------------------------------------------------------------------
    @Nested
    @DisplayName("rejectRequest()")
    class RejectRequestTests {

        @Test
        @DisplayName("should reject with reason")
        void shouldRejectWithReason() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.WAITING_FOR_APPROVAL, MAKER_ID);
            setCurrentUser(CHECKER_ID);

            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));
            when(userRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = userRequestService.rejectRequest(requestId, "Missing documents");

            assertThat(result.getStatus()).isEqualTo(UserRequestStatus.REJECTED);
            assertThat(result.getStatusChangeReason()).isEqualTo("Missing documents");
            verify(eventPublisher).publishEvent(any(UserRequestService.UserRequestProcessedEvent.class));
        }

        @Test
        @DisplayName("should throw when reason is null")
        void shouldThrowWhenReasonIsNull() {
            setCurrentUser(CHECKER_ID);

            assertThatThrownBy(() -> userRequestService.rejectRequest(UUID.randomUUID(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reject reason is mandatory");
        }

        @Test
        @DisplayName("should throw when reason is blank")
        void shouldThrowWhenReasonIsBlank() {
            setCurrentUser(CHECKER_ID);

            assertThatThrownBy(() -> userRequestService.rejectRequest(UUID.randomUUID(), "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reject reason is mandatory");
        }

        @Test
        @DisplayName("should enforce separation of duties on reject")
        void shouldEnforceSeparationOfDutiesOnReject() {
            var requestId = UUID.randomUUID();
            var request = createRequest(requestId, UserRequestStatus.WAITING_FOR_APPROVAL, MAKER_ID);
            setCurrentUser(MAKER_ID);

            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> userRequestService.rejectRequest(requestId, "Bad data"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Separation of duties");
        }
    }

    // -------------------------------------------------------------------
    // Full Workflow
    // -------------------------------------------------------------------
    @Nested
    @DisplayName("Full Maker/Checker Workflow")
    class FullWorkflowTests {

        @Test
        @DisplayName("should complete full create -> submit -> approve flow")
        void shouldCompleteFullFlow() {
            var requestId = UUID.randomUUID();
            var roleId = UUID.randomUUID();

            // Step 1: Create (Maker)
            setCurrentUser(MAKER_ID);
            when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
            when(userRequestRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
            when(roleRepository.existsById(roleId)).thenReturn(true);
            when(userRequestRepository.save(any(UserRequest.class))).thenAnswer(inv -> {
                var req = inv.getArgument(0, UserRequest.class);
                if (req.getId() == null)
                    req.setId(requestId);
                return req;
            });
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var created = userRequestService.createRequest(
                    "newuser@example.com", "New", "User", Set.of(roleId));
            assertThat(created.getStatus()).isEqualTo(UserRequestStatus.DRAFT);

            // Step 2: Submit (Maker)
            when(userRequestRepository.findByIdAndNotDeleted(requestId)).thenReturn(Optional.of(created));
            var submitted = userRequestService.submitRequest(requestId);
            assertThat(submitted.getStatus()).isEqualTo(UserRequestStatus.WAITING_FOR_APPROVAL);

            // Step 3: Approve (Checker - different user)
            setCurrentUser(CHECKER_ID);
            var role = new Role();
            role.setId(roleId);
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                var user = inv.getArgument(0, User.class);
                user.setId(UUID.randomUUID());
                return user;
            });

            var approved = userRequestService.approveRequest(requestId);
            assertThat(approved.getStatus()).isEqualTo(UserRequestStatus.APPROVED);

            // Verify: User was created, event was published
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(UserRequestService.UserRequestProcessedEvent.class));
        }
    }
}
