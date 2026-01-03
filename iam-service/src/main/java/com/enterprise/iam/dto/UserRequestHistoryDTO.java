package com.enterprise.iam.dto;

import com.enterprise.iam.entity.UserRequestAction;
import com.enterprise.iam.entity.UserRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * User Request History DTO
 * <p>
 * DTO for UserRequestHistory entity to expose audit log.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User request history entry")
public class UserRequestHistoryDTO {

    @Schema(description = "History entry ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Request ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID requestId;

    @Schema(description = "Previous status", example = "DRAFT")
    private UserRequestStatus oldStatus;

    @Schema(description = "New status", example = "WAITING_FOR_APPROVAL")
    private UserRequestStatus newStatus;

    @Schema(description = "Action performed", example = "SUBMIT")
    private UserRequestAction action;

    @Schema(description = "User ID who performed the action", example = "user@example.com")
    private String actorId;

    @Schema(description = "Comment/reason", example = "Request submitted for approval")
    private String comment;

    @Schema(description = "Metadata (JSON)", example = "{\"email\": \"old@example.com\"}")
    private String metadata;

    @Schema(description = "When action was performed", example = "2025-01-01T00:00:00Z")
    private Instant createdAt;
}

