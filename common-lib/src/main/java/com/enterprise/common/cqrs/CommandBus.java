package com.enterprise.common.cqrs;

/**
 * Command Bus Interface
 *
 * Central dispatcher for all commands in the system.
 * Responsible for:
 * - Finding the correct CommandHandler for a given Command
 * - Executing the handler
 * - Managing transaction boundaries
 * - Applying validation and middleware
 *
 * Architecture:
 * <pre>
 * Controller
 *   ↓
 * commandBus.dispatch(CreateUserCommand)
 *   ↓
 * [Validation Pipeline]
 *   ↓
 * [Transaction Management]
 *   ↓
 * CreateUserCommandHandler.handle(command)
 *   ↓
 * [Event Publishing]
 *   ↓
 * Return Result
 * </pre>
 *
 * Benefits:
 * - Decouples controllers from handlers (no direct dependencies)
 * - Centralized cross-cutting concerns (validation, logging, transactions)
 * - Easier testing (mock the bus, not individual handlers)
 * - Enables middleware and interceptors
 *
 * Usage in Controller:
 * <pre>
 * @RestController
 * @RequiredArgsConstructor
 * public class UserController {
 *
 *     private final CommandBus commandBus;
 *     private final QueryBus queryBus;
 *
 *     @PostMapping("/users")
 *     public ResponseEntity<ApiResponse<UUID>> createUser(@RequestBody CreateUserRequest request) {
 *         CreateUserCommand command = new CreateUserCommand(
 *             request.email(),
 *             request.firstName(),
 *             request.lastName()
 *         );
 *
 *         UUID userId = commandBus.dispatch(command);
 *
 *         return ResponseEntity.ok(ApiResponse.success(userId));
 *     }
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
 * @author Enterprise Team
 * @since 1.0.0
 */
public interface CommandBus {

    /**
     * Dispatch a command to its handler
     *
     * Flow:
     * 1. Find CommandHandler<C, R> for the given command type
     * 2. Validate command (JSR-303 annotations)
     * 3. Execute handler.handle(command) within transaction
     * 4. Publish domain events if any
     * 5. Return result
     *
     * @param command The command to dispatch
     * @param <C>     The command type
     * @param <R>     The result type
     * @return The result of executing the command
     * @throws IllegalArgumentException if no handler found for command
     * @throws ValidationException      if command validation fails
     */
    <C extends Command<R>, R> R dispatch(C command);
}
