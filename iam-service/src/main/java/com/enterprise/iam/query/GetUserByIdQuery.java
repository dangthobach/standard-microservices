package com.enterprise.iam.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.iam.dto.UserDTO;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Get User By ID Query
 *
 * Represents a request to fetch a user by their ID.
 * This is a READ operation that does not change system state.
 *
 * Validation:
 * - userId: Must not be null
 *
 * Usage:
 * <pre>
 * @RestController
 * @RequiredArgsConstructor
 * public class UserController {
 *
 *     private final QueryBus queryBus;
 *
 *     @GetMapping("/users/{id}")
 *     public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
 *         GetUserByIdQuery query = new GetUserByIdQuery(id);
 *         UserDTO user = queryBus.dispatch(query);
 *         return ResponseEntity.ok(ApiResponse.success(user));
 *     }
 * }
 * </pre>
 *
 * @param userId The ID of the user to retrieve
 * @author Enterprise Team
 * @since 1.0.0
 */
public record GetUserByIdQuery(
    @NotNull(message = "User ID is required")
    UUID userId
) implements Query<UserDTO> {
}
