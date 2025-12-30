package com.enterprise.common.cqrs;

/**
 * Base marker interface for all Commands
 *
 * Commands represent write operations (CREATE, UPDATE, DELETE) that change system state.
 *
 * CQRS Pattern Benefits:
 * - Clear separation between reads (Query) and writes (Command)
 * - Easier to implement different validation/authorization for writes
 * - Enables event sourcing and audit logging
 * - Allows separate scaling of read and write paths
 *
 * Usage:
 * <pre>
 * public record CreateUserCommand(
 *     String email,
 *     String firstName,
 *     String lastName
 * ) implements Command<UUID> {}
 *
 * @Service
 * public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UUID> {
 *     public UUID handle(CreateUserCommand command) {
 *         User user = User.builder()
 *             .email(command.email())
 *             .firstName(command.firstName())
 *             .lastName(command.lastName())
 *             .build();
 *         return userRepository.save(user).getId();
 *     }
 * }
 * </pre>
 *
 * @param <R> The type of result returned after executing the command
 */
public interface Command<R> {
    // Marker interface - no methods
    // The generic type R represents the command result (e.g., UUID for entity ID)
}
