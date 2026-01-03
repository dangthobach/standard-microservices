package com.enterprise.iam.dto;

import com.enterprise.iam.entity.UserRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * User Request DTO
 * <p>
 * DTO for UserRequest entity to expose to API clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User creation request information")
public class UserRequestDTO {

    @Schema(description = "Request ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Email address for new user", example = "john.doe@enterprise.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Role IDs to assign", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    private Set<UUID> roleIds;

    @Schema(description = "Request status", example = "WAITING_FOR_APPROVAL")
    private UserRequestStatus status;

    @Schema(description = "ID of user who created this request (Maker)", example = "maker@example.com")
    private String requestCreatorId;

    @Schema(description = "Status change reason", example = "Request approved")
    private String statusChangeReason;

    @Schema(description = "When status was last changed", example = "2025-01-01T00:00:00Z")
    private Instant statusChangedAt;

    @Schema(description = "Who changed the status", example = "checker@example.com")
    private String statusChangedBy;

    @Schema(description = "Request creation timestamp", example = "2025-01-01T00:00:00Z")
    private Instant createdAt;

    @Schema(description = "Request last update timestamp", example = "2025-01-01T00:00:00Z")
    private Instant updatedAt;
}

