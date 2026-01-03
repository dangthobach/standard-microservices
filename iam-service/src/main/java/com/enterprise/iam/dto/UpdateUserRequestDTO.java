package com.enterprise.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Update User Request DTO
 * <p>
 * DTO for updating an existing user request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a user request")
public class UpdateUserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Email address for new user", example = "john.doe@enterprise.com")
    private String email;

    @NotBlank(message = "First name is required")
    @Schema(description = "First name", example = "John")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Role IDs to assign", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    private Set<UUID> roleIds;
}

