package com.enterprise.iam.command;

import com.enterprise.common.cqrs.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Create User Command
 *
 * Represents a request to create a new user in the system.
 * This is a WRITE operation that changes system state.
 *
 * Validation:
 * - email: Must be valid email format and not blank
 * - firstName: 2-50 characters, not blank
 * - lastName: 2-50 characters, not blank
 * - keycloakId: Not blank
 *
 * Usage:
 * <pre>
 * @RestController
 * @RequiredArgsConstructor
 * public class UserController {
 *
 *     private final CommandBus commandBus;
 *
 *     @PostMapping("/users")
 *     public ResponseEntity<ApiResponse<UUID>> createUser(@RequestBody CreateUserRequest request) {
 *         CreateUserCommand command = new CreateUserCommand(
 *             request.keycloakId(),
 *             request.email(),
 *             request.firstName(),
 *             request.lastName()
 *         );
 *
 *         UUID userId = commandBus.dispatch(command);
 *
 *         return ResponseEntity.ok(ApiResponse.success(userId));
 *     }
 * }
 * </pre>
 *
 * @param keycloakId  The Keycloak user ID
 * @param email       User's email address
 * @param firstName   User's first name
 * @param lastName    User's last name
 * @author Enterprise Team
 * @since 1.0.0
 */
public record CreateUserCommand(
    @NotBlank(message = "Keycloak ID is required")
    String keycloakId,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName
) implements Command<UUID> {
}
