package com.enterprise.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Batch Reject DTO
 * <p>
 * DTO for batch rejecting multiple requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to batch reject user requests")
public class BatchRejectDTO {

    @NotEmpty(message = "Request IDs list cannot be empty")
    @Schema(description = "List of request IDs to reject", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    private List<UUID> requestIds;

    @NotBlank(message = "Reject reason is mandatory")
    @Schema(description = "Reason for rejection", example = "Incomplete information")
    private String reason;
}

