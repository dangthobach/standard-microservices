package com.enterprise.common.cqrs;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Command Bus Implementation
 *
 * Simple, high-performance implementation of CommandBus using a ConcurrentHashMap
 * to store Command -> CommandHandler mappings.
 *
 * Features:
 * - Auto-registration of handlers via constructor injection
 * - JSR-303 validation before execution
 * - Transaction management via @Transactional
 * - Thread-safe handler lookup
 * - Fast O(1) handler resolution
 *
 * How Handlers Are Registered:
 * 1. Spring scans for beans implementing CommandHandler<C, R>
 * 2. CqrsConfiguration collects all handlers and passes to this class
 * 3. Handlers are registered in a Map<Class<? extends Command>, CommandHandler>
 * 4. dispatch() does O(1) lookup by command.getClass()
 *
 * Performance:
 * - Handler lookup: O(1) constant time
 * - Validation: ~10-100µs depending on constraints
 * - Overhead: < 1ms per command
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class InMemoryCommandBus implements CommandBus {

    private final Map<Class<? extends Command<?>>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final Validator validator;

    public InMemoryCommandBus(Validator validator) {
        this.validator = validator;
        log.info("✅ InMemoryCommandBus initialized");
    }

    /**
     * Register a command handler
     *
     * Called by CqrsConfiguration during application startup.
     * Uses reflection to extract the command type from the handler's generic type.
     *
     * @param commandClass The command class
     * @param handler      The handler for this command
     * @param <C>          The command type
     * @param <R>          The result type
     */
    public <C extends Command<R>, R> void registerHandler(Class<C> commandClass, CommandHandler<C, R> handler) {
        if (handlers.containsKey(commandClass)) {
            log.warn("⚠️ Duplicate handler for command: {}. Overwriting previous handler.", commandClass.getSimpleName());
        }

        handlers.put(commandClass, handler);
        log.debug("✅ Registered handler: {} -> {}", commandClass.getSimpleName(), handler.getClass().getSimpleName());
    }

    /**
     * Dispatch a command to its handler
     *
     * Flow:
     * 1. Validate command using JSR-303 annotations
     * 2. Find handler for command type
     * 3. Execute handler.handle(command)
     * 4. Return result
     *
     * @param command The command to dispatch
     * @param <C>     The command type
     * @param <R>     The result type
     * @return The result of executing the command
     */
    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public <C extends Command<R>, R> R dispatch(C command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        // Step 1: Validate command
        validateCommand(command);

        // Step 2: Find handler
        Class<? extends Command<?>> commandClass = (Class<? extends Command<?>>) command.getClass();
        CommandHandler<C, R> handler = (CommandHandler<C, R>) handlers.get(commandClass);

        if (handler == null) {
            throw new IllegalArgumentException(
                "No handler registered for command: " + commandClass.getSimpleName() +
                ". Did you forget to create a @Service that implements CommandHandler<" +
                commandClass.getSimpleName() + ", ?>?"
            );
        }

        // Step 3: Execute handler
        log.debug("📤 Dispatching command: {} -> {}", commandClass.getSimpleName(), handler.getClass().getSimpleName());

        long startTime = System.currentTimeMillis();
        R result = handler.handle(command);
        long duration = System.currentTimeMillis() - startTime;

        log.debug("✅ Command executed: {} in {}ms", commandClass.getSimpleName(), duration);

        if (duration > 1000) {
            log.warn("⚠️ Slow command execution: {} took {}ms (>1s)", commandClass.getSimpleName(), duration);
        }

        return result;
    }

    /**
     * Validate command using JSR-303 Bean Validation
     *
     * Throws IllegalArgumentException if validation fails.
     *
     * Example:
     * <pre>
     * public record CreateUserCommand(
     *     @NotBlank @Email String email,
     *     @NotBlank @Size(min = 2, max = 50) String firstName,
     *     @NotBlank @Size(min = 2, max = 50) String lastName
     * ) implements Command<UUID> {}
     * </pre>
     *
     * @param command The command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private <C extends Command<?>> void validateCommand(C command) {
        Set<ConstraintViolation<C>> violations = validator.validate(command);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Unknown validation error");

            throw new IllegalArgumentException("Command validation failed: " + errors);
        }
    }

    /**
     * Get the number of registered handlers
     *
     * Useful for testing and debugging.
     *
     * @return Number of registered handlers
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
