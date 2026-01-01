package com.enterprise.common.config;

import com.enterprise.common.cqrs.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * CQRS Auto-Configuration
 *
 * Automatically discovers and registers all CommandHandlers and QueryHandlers
 * in the Spring application context.
 *
 * How It Works:
 * 1. Spring Boot starts and creates all beans
 * 2. @PostConstruct triggers after bean creation
 * 3. ApplicationContext.getBeansOfType() finds all CommandHandler beans
 * 4. For each handler, extract generic type (Command class) using reflection
 * 5. Register handler in InMemoryCommandBus
 * 6. Repeat for QueryHandlers
 *
 * Example:
 * <pre>
 * @Service
 * public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UUID> {
 *     @Override
 *     public UUID handle(CreateUserCommand command) {
 *         // Implementation
 *     }
 * }
 * </pre>
 *
 * This handler will be automatically discovered and registered.
 * No manual configuration needed!
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class CqrsConfiguration {

    private final ApplicationContext applicationContext;
    private final InMemoryCommandBus commandBus;
    private final InMemoryQueryBus queryBus;

    public CqrsConfiguration(
        ApplicationContext applicationContext,
        InMemoryCommandBus commandBus,
        InMemoryQueryBus queryBus
    ) {
        this.applicationContext = applicationContext;
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    /**
     * Auto-register all CommandHandlers and QueryHandlers
     *
     * Runs after Spring Boot startup, before accepting requests.
     */
    @PostConstruct
    public void registerHandlers() {
        log.info("🔧 Auto-registering CQRS handlers...");

        int commandHandlers = registerCommandHandlers();
        int queryHandlers = registerQueryHandlers();

        log.info("✅ CQRS initialization complete: {} command handlers, {} query handlers",
            commandHandlers, queryHandlers);

        if (commandHandlers == 0 && queryHandlers == 0) {
            log.warn("⚠️ No CQRS handlers found. Did you create any @Service classes implementing CommandHandler or QueryHandler?");
        }
    }

    /**
     * Find and register all CommandHandlers
     *
     * @return Number of handlers registered
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private int registerCommandHandlers() {
        Map<String, CommandHandler> handlers = applicationContext.getBeansOfType(CommandHandler.class);

        for (Map.Entry<String, CommandHandler> entry : handlers.entrySet()) {
            CommandHandler<?, ?> handler = entry.getValue();

            // Extract generic types from CommandHandler<C, R>
            ResolvableType resolvableType = ResolvableType.forClass(handler.getClass()).as(CommandHandler.class);
            ResolvableType[] generics = resolvableType.getGenerics();

            if (generics.length == 2) {
                Class<?> commandClass = generics[0].resolve();

                if (commandClass != null && Command.class.isAssignableFrom(commandClass)) {
                    commandBus.registerHandler((Class) commandClass, handler);
                } else {
                    log.warn("⚠️ Could not resolve command type for handler: {}", handler.getClass().getSimpleName());
                }
            }
        }

        return handlers.size();
    }

    /**
     * Find and register all QueryHandlers
     *
     * @return Number of handlers registered
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private int registerQueryHandlers() {
        Map<String, QueryHandler> handlers = applicationContext.getBeansOfType(QueryHandler.class);

        for (Map.Entry<String, QueryHandler> entry : handlers.entrySet()) {
            QueryHandler<?, ?> handler = entry.getValue();

            // Extract generic types from QueryHandler<Q, R>
            ResolvableType resolvableType = ResolvableType.forClass(handler.getClass()).as(QueryHandler.class);
            ResolvableType[] generics = resolvableType.getGenerics();

            if (generics.length == 2) {
                Class<?> queryClass = generics[0].resolve();

                if (queryClass != null && Query.class.isAssignableFrom(queryClass)) {
                    queryBus.registerHandler((Class) queryClass, handler);
                } else {
                    log.warn("⚠️ Could not resolve query type for handler: {}", handler.getClass().getSimpleName());
                }
            }
        }

        return handlers.size();
    }
}
