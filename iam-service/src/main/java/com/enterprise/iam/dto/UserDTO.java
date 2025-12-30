package com.enterprise.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * User Data Transfer Object
 * <p>
 * DTO for User entity to expose to API clients.
 * Excludes sensitive fields and internal IDs.
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information")
public class UserDTO {

    @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Email address", example = "john.doe@enterprise.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "User is enabled", example = "true")
    private Boolean enabled;

    @Schema(description = "Email is verified", example = "true")
    private Boolean emailVerified;

    @Schema(description = "User roles", example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    private Set<String> roles;

    @Schema(description = "Last login timestamp", example = "2025-12-31T00:00:00Z")
    private Instant lastLoginAt;

    @Schema(description = "Account creation timestamp", example = "2025-01-01T00:00:00Z")
    private Instant createdAt;
}
