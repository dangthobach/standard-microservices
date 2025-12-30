package com.enterprise.iam.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.iam.dto.UserDTO;
import com.enterprise.iam.entity.User;
import com.enterprise.iam.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * User Management Controller
 * <p>
 * Demonstrates standardized API response patterns with:
 * - ApiResponse wrapper for all endpoints
 * - PageResponse for paginated results
 * - Automatic tracing (traceId, spanId) via GlobalResponseBodyAdvice
 * - Automatic exception handling via GlobalExceptionHandler
 * <p>
 * Example responses:
 * <pre>
 * Success:
 * {
 *   "success": true,
 *   "message": "User retrieved successfully",
 *   "data": { "id": "...", "email": "..." },
 *   "traceId": "5f9c8a7b6d4e3f2a1b0c9d8e",
 *   "spanId": "1a2b3c4d5e6f7g8h",
 *   "timestamp": "2025-12-31T00:00:00Z"
 * }
 *
 * Error:
 * {
 *   "success": false,
 *   "message": "User not found",
 *   "errorCode": "USER_NOT_FOUND",
 *   "traceId": "5f9c8a7b6d4e3f2a1b0c9d8e",
 *   "spanId": "1a2b3c4d5e6f7g8h",
 *   "timestamp": "2025-12-31T00:00:00Z"
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD operations")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get all users with pagination.
     * <p>
     * Example: GET /api/users?page=0&size=20
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated list of users wrapped in ApiResponse
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve paginated list of users")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching users: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        // Convert to DTOs
        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResponse<UserDTO> pageResponse = PageResponse.from(userPage, userDTOs);

        // ApiResponse automatically gets traceId/spanId via GlobalResponseBodyAdvice
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", pageResponse)
        );
    }

    /**
     * Get user by ID.
     * <p>
     * Example: GET /api/users/550e8400-e29b-41d4-a716-446655440000
     *
     * @param id User ID
     * @return User details wrapped in ApiResponse
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable UUID id) {
        log.info("Fetching user by ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        UserDTO userDTO = convertToDTO(user);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userDTO)
        );
    }

    /**
     * Search users by email.
     * <p>
     * Example: GET /api/users/search?email=john@example.com
     *
     * @param email Email to search
     * @return User details wrapped in ApiResponse
     */
    @GetMapping("/search")
    @Operation(summary = "Search user by email", description = "Find user by email address")
    public ResponseEntity<ApiResponse<UserDTO>> searchByEmail(
            @RequestParam String email
    ) {
        log.info("Searching user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        UserDTO userDTO = convertToDTO(user);

        return ResponseEntity.ok(
                ApiResponse.success("User found", userDTO)
        );
    }

    /**
     * Get user statistics.
     * <p>
     * Example: GET /api/users/stats
     *
     * @return User statistics wrapped in ApiResponse
     */
    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Retrieve user counts and statistics")
    public ResponseEntity<ApiResponse<UserStats>> getUserStats() {
        log.info("Fetching user statistics");

        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.countByEnabled(true);
        long disabledUsers = totalUsers - enabledUsers;

        UserStats stats = UserStats.builder()
                .totalUsers(totalUsers)
                .enabledUsers(enabledUsers)
                .disabledUsers(disabledUsers)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("User statistics retrieved successfully", stats)
        );
    }

    /**
     * Example endpoint that demonstrates automatic exception handling.
     * <p>
     * This will trigger GlobalExceptionHandler if user not found.
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "Get user roles", description = "Retrieve roles assigned to user")
    public ResponseEntity<ApiResponse<List<String>>> getUserRoles(@PathVariable UUID id) {
        log.info("Fetching roles for user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("User roles retrieved successfully", roles)
        );
    }

    /**
     * Convert User entity to UserDTO.
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * User statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "User statistics")
    public static class UserStats {
        @Schema(description = "Total number of users", example = "1000")
        private long totalUsers;

        @Schema(description = "Number of enabled users", example = "950")
        private long enabledUsers;

        @Schema(description = "Number of disabled users", example = "50")
        private long disabledUsers;
    }
}
