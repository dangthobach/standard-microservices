package com.enterprise.common.cqrs;

/**
 * Interface for handling Commands
 *
 * Implements the Command pattern for write operations.
 * Each CommandHandler is responsible for:
 * - Validating the command
 * - Executing business logic
 * - Persisting changes
 * - Returning result
 *
 * Best Practices:
 * - One handler per command type
 * - Handler should be transactional (@Transactional)
 * - Emit domain events for important state changes
 * - Return only essential data (e.g., entity ID, not full entity)
 *
 * Usage:
 * <pre>
 * @Service
 * @Transactional
 * public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UUID> {
 *
 *     private final UserRepository userRepository;
 *     private final ApplicationEventPublisher eventPublisher;
 *
 *     @Override
 *     public UUID handle(CreateUserCommand command) {
 *         // Validate
 *         if (userRepository.existsByEmail(command.email())) {
 *             throw new DuplicateEmailException();
 *         }
 *
 *         // Execute
 *         User user = User.builder()
 *             .email(command.email())
 *             .firstName(command.firstName())
 *             .lastName(command.lastName())
 *             .build();
 *
 *         user = userRepository.save(user);
 *
 *         // Emit event
 *         eventPublisher.publishEvent(new UserCreatedEvent(user.getId()));
 *
 *         return user.getId();
 *     }
 * }
 * </pre>
 *
 * @param <C> The command type
 * @param <R> The result type
 */
public interface CommandHandler<C extends Command<R>, R> {

    /**
     * Handle the command and return result
     *
     * @param command The command to handle
     * @return The result of executing the command
     */
    R handle(C command);
}
