package com.enterprise.iam.command;

import com.enterprise.common.cqrs.CommandHandler;
import com.enterprise.iam.entity.User;
import com.enterprise.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Create User Command Handler
 *
 * Handles the CreateUserCommand by:
 * 1. Validating that email and keycloakId are unique
 * 2. Creating and persisting the User entity
 * 3. Publishing UserCreatedEvent for other services to react
 * 4. Returning the new user's ID
 *
 * Transaction Management:
 * - @Transactional ensures all database operations are atomic
 * - If any exception occurs, entire transaction is rolled back
 *
 * Event Publishing:
 * - Publishes UserCreatedEvent after successful creation
 * - Other services can listen to this event (e.g., send welcome email)
 *
 * Example Event Listener:
 * <pre>
 * @Component
 * public class UserCreatedEventListener {
 *
 *     @EventListener
 *     public void handleUserCreated(UserCreatedEvent event) {
 *         // Send welcome email
 *         // Create user profile
 *         // Provision resources
 *     }
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UUID> {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handle CreateUserCommand
     *
     * @param command The command containing user data
     * @return The ID of the newly created user
     * @throws IllegalArgumentException if email or keycloakId already exists
     */
    @Override
    @Transactional
    public UUID handle(CreateUserCommand command) {
        log.debug("Handling CreateUserCommand: email={}", command.email());

        // Step 1: Validate uniqueness
        validateUniqueness(command);

        // Step 2: Create user entity
        User user = User.builder()
            .keycloakId(command.keycloakId())
            .email(command.email())
            .firstName(command.firstName())
            .lastName(command.lastName())
            .enabled(true)
            .emailVerified(false)
            .build();

        // Step 3: Persist to database
        user = userRepository.save(user);

        log.info("✅ User created: id={}, email={}", user.getId(), user.getEmail());

        // Step 4: Publish domain event
        publishUserCreatedEvent(user);

        return user.getId();
    }

    /**
     * Validate that email and keycloakId are unique
     *
     * @param command The command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUniqueness(CreateUserCommand command) {
        // Check email uniqueness
        if (userRepository.existsByEmailAndDeletedFalse(command.email())) {
            throw new IllegalArgumentException(
                "User with email '" + command.email() + "' already exists"
            );
        }

        // Check keycloakId uniqueness
        if (userRepository.existsByKeycloakIdAndDeletedFalse(command.keycloakId())) {
            throw new IllegalArgumentException(
                "User with Keycloak ID '" + command.keycloakId() + "' already exists"
            );
        }
    }

    /**
     * Publish UserCreatedEvent for other components to react
     *
     * Other services can listen to this event via @EventListener.
     *
     * @param user The newly created user
     */
    private void publishUserCreatedEvent(User user) {
        UserCreatedEvent event = new UserCreatedEvent(
            user.getId(),
            user.getEmail(),
            user.getFullName()
        );

        eventPublisher.publishEvent(event);
        log.debug("📢 Published UserCreatedEvent: userId={}", user.getId());
    }

    /**
     * Domain Event: User Created
     *
     * Published after a user is successfully created.
     * Other components can listen to this event to perform follow-up actions.
     *
     * @param userId   The ID of the created user
     * @param email    The user's email
     * @param fullName The user's full name
     */
    public record UserCreatedEvent(
        UUID userId,
        String email,
        String fullName
    ) {
    }
}
