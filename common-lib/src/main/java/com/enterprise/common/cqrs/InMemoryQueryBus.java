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
 * In-Memory Query Bus Implementation
 *
 * Simple, high-performance implementation of QueryBus using a ConcurrentHashMap
 * to store Query -> QueryHandler mappings.
 *
 * Features:
 * - Auto-registration of handlers via constructor injection
 * - JSR-303 validation before execution
 * - Read-only transaction management
 * - Thread-safe handler lookup
 * - Fast O(1) handler resolution
 *
 * How Handlers Are Registered:
 * 1. Spring scans for beans implementing QueryHandler<Q, R>
 * 2. CqrsConfiguration collects all handlers and passes to this class
 * 3. Handlers are registered in a Map<Class<? extends Query>, QueryHandler>
 * 4. dispatch() does O(1) lookup by query.getClass()
 *
 * Performance:
 * - Handler lookup: O(1) constant time
 * - Validation: ~10-100µs depending on constraints
 * - Overhead: < 1ms per query
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class InMemoryQueryBus implements QueryBus {

    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final Validator validator;

    public InMemoryQueryBus(Validator validator) {
        this.validator = validator;
        log.info("✅ InMemoryQueryBus initialized");
    }

    /**
     * Register a query handler
     *
     * Called by CqrsConfiguration during application startup.
     * Uses reflection to extract the query type from the handler's generic type.
     *
     * @param queryClass The query class
     * @param handler    The handler for this query
     * @param <Q>        The query type
     * @param <R>        The result type
     */
    public <Q extends Query<R>, R> void registerHandler(Class<Q> queryClass, QueryHandler<Q, R> handler) {
        if (handlers.containsKey(queryClass)) {
            log.warn("⚠️ Duplicate handler for query: {}. Overwriting previous handler.", queryClass.getSimpleName());
        }

        handlers.put(queryClass, handler);
        log.debug("✅ Registered handler: {} -> {}", queryClass.getSimpleName(), handler.getClass().getSimpleName());
    }

    /**
     * Dispatch a query to its handler
     *
     * Flow:
     * 1. Validate query using JSR-303 annotations
     * 2. Find handler for query type
     * 3. Execute handler.handle(query) (read-only transaction)
     * 4. Return result
     *
     * @param query The query to dispatch
     * @param <Q>   The query type
     * @param <R>   The result type
     * @return The result of executing the query
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <Q extends Query<R>, R> R dispatch(Q query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        // Step 1: Validate query
        validateQuery(query);

        // Step 2: Find handler
        Class<? extends Query<?>> queryClass = (Class<? extends Query<?>>) query.getClass();
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(queryClass);

        if (handler == null) {
            throw new IllegalArgumentException(
                "No handler registered for query: " + queryClass.getSimpleName() +
                ". Did you forget to create a @Service that implements QueryHandler<" +
                queryClass.getSimpleName() + ", ?>?"
            );
        }

        // Step 3: Execute handler
        log.debug("🔍 Dispatching query: {} -> {}", queryClass.getSimpleName(), handler.getClass().getSimpleName());

        long startTime = System.currentTimeMillis();
        R result = handler.handle(query);
        long duration = System.currentTimeMillis() - startTime;

        log.debug("✅ Query executed: {} in {}ms", queryClass.getSimpleName(), duration);

        if (duration > 500) {
            log.warn("⚠️ Slow query execution: {} took {}ms (>500ms). Consider caching or optimization.",
                queryClass.getSimpleName(), duration);
        }

        return result;
    }

    /**
     * Validate query using JSR-303 Bean Validation
     *
     * Throws IllegalArgumentException if validation fails.
     *
     * Example:
     * <pre>
     * public record GetUserByIdQuery(
     *     @NotNull UUID userId
     * ) implements Query<UserDTO> {}
     * </pre>
     *
     * @param query The query to validate
     * @throws IllegalArgumentException if validation fails
     */
    private <Q extends Query<?>> void validateQuery(Q query) {
        Set<ConstraintViolation<Q>> violations = validator.validate(query);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Unknown validation error");

            throw new IllegalArgumentException("Query validation failed: " + errors);
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
