package com.enterprise.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Batch Approve DTO
 * <p>
 * DTO for batch approving multiple requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to batch approve user requests")
public class BatchApproveDTO {

    @NotEmpty(message = "Request IDs list cannot be empty")
    @Schema(description = "List of request IDs to approve", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    private List<UUID> requestIds;
}

