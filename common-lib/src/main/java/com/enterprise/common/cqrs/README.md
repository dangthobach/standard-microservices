# CQRS Infrastructure

**Production-Ready Command Query Responsibility Segregation (CQRS) Pattern Implementation**

## Overview

This CQRS infrastructure provides a clean separation between read operations (Queries) and write operations (Commands) in your microservices architecture.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller Layer                       │
│                                                              │
│  @PostMapping("/users")                                     │
│  public ResponseEntity<ApiResponse<UUID>> createUser() {    │
│      CreateUserCommand command = ...                        │
│      UUID userId = commandBus.dispatch(command);            │
│      return ResponseEntity.ok(success(userId));             │
│  }                                                           │
│                                                              │
│  @GetMapping("/users/{id}")                                 │
│  public ResponseEntity<ApiResponse<UserDTO>> getUser() {    │
│      GetUserByIdQuery query = ...                           │
│      UserDTO user = queryBus.dispatch(query);               │
│      return ResponseEntity.ok(success(user));               │
│  }                                                           │
└──────────────────┬────────────────────┬─────────────────────┘
                   │                    │
                   ↓                    ↓
        ┌──────────────────┐  ┌──────────────────┐
        │   CommandBus     │  │    QueryBus      │
        │ (Write Path)     │  │  (Read Path)     │
        └────────┬─────────┘  └────────┬─────────┘
                 │                     │
                 ↓                     ↓
        ┌──────────────────┐  ┌──────────────────┐
        │ CommandHandler   │  │  QueryHandler    │
        │  - Validate      │  │  - Fetch Data    │
        │  - Execute       │  │  - Cache         │
        │  - Persist       │  │  - Transform     │
        │  - Emit Events   │  │  - Return DTO    │
        └────────┬─────────┘  └────────┬─────────┘
                 │                     │
                 ↓                     ↓
        ┌──────────────────┐  ┌──────────────────┐
        │    Database      │  │   Read Cache     │
        │   (Write Path)   │  │  (Optimized)     │
        └──────────────────┘  └──────────────────┘
```

## Components

### 1. Core Interfaces

#### `Command<R>`
Marker interface for all write operations.

```java
public interface Command<R> {
    // R = Result type (e.g., UUID for entity ID)
}
```

#### `CommandHandler<C, R>`
Handler for executing commands.

```java
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}
```

#### `Query<R>`
Marker interface for all read operations.

```java
public interface Query<R> {
    // R = Result type (e.g., UserDTO)
}
```

#### `QueryHandler<Q, R>`
Handler for executing queries.

```java
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
```

### 2. Bus Implementations

#### `InMemoryCommandBus`
- O(1) handler lookup via ConcurrentHashMap
- Automatic JSR-303 validation
- Transaction management (`@Transactional`)
- Performance monitoring (logs slow commands >1s)

#### `InMemoryQueryBus`
- O(1) handler lookup via ConcurrentHashMap
- Automatic JSR-303 validation
- Read-only transactions (`@Transactional(readOnly = true)`)
- Performance monitoring (logs slow queries >500ms)

### 3. Auto-Configuration

#### `CqrsConfiguration`
- Automatically discovers all `CommandHandler` and `QueryHandler` beans
- Registers them in respective buses at application startup
- Uses Spring's `ResolvableType` for generic type extraction
- Zero manual configuration required!

## Usage Guide

### Step 1: Create a Command

```java
package com.enterprise.iam.command;

import com.enterprise.common.cqrs.Command;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateUserCommand(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    String lastName
) implements Command<UUID> {
}
```

### Step 2: Create a CommandHandler

```java
package com.enterprise.iam.command;

import com.enterprise.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, UUID> {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UUID handle(CreateUserCommand command) {
        // 1. Validate uniqueness
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 2. Create and persist
        User user = User.builder()
            .email(command.email())
            .firstName(command.firstName())
            .lastName(command.lastName())
            .build();

        user = userRepository.save(user);

        // 3. Publish event
        eventPublisher.publishEvent(new UserCreatedEvent(user.getId()));

        return user.getId();
    }
}
```

### Step 3: Create a Query

```java
package com.enterprise.iam.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.iam.dto.UserDTO;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GetUserByIdQuery(
    @NotNull(message = "User ID is required")
    UUID userId
) implements Query<UserDTO> {
}
```

### Step 4: Create a QueryHandler

```java
package com.enterprise.iam.query;

import com.enterprise.common.cqrs.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserByIdQueryHandler implements QueryHandler<GetUserByIdQuery, UserDTO> {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#query.userId")
    public UserDTO handle(GetUserByIdQuery query) {
        User user = userRepository.findById(query.userId())
            .filter(User::isActive)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserDTO.from(user);
    }
}
```

### Step 5: Use in Controller

```java
package com.enterprise.iam.controller;

import com.enterprise.common.cqrs.CommandBus;
import com.enterprise.common.cqrs.QueryBus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createUser(@RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
            request.email(),
            request.firstName(),
            request.lastName()
        );

        UUID userId = commandBus.dispatch(command);

        return ResponseEntity.ok(ApiResponse.success(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
        GetUserByIdQuery query = new GetUserByIdQuery(id);
        UserDTO user = queryBus.dispatch(query);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
```

## Benefits

### 1. **Separation of Concerns**
- Commands handle writes (state changes)
- Queries handle reads (no side effects)
- Clear boundaries between read and write logic

### 2. **Testability**
- Easy to unit test handlers in isolation
- Mock the bus in controller tests
- No tight coupling between controllers and handlers

### 3. **Scalability**
- Optimize read and write paths independently
- Add caching to queries without affecting commands
- Scale read replicas separately from write databases

### 4. **Maintainability**
- One handler per command/query (Single Responsibility)
- Easy to find and modify business logic
- Clear naming conventions (CreateUserCommand → CreateUserCommandHandler)

### 5. **Extensibility**
- Add middleware/interceptors easily (logging, metrics, validation)
- Implement cross-cutting concerns in one place
- Support multiple databases or event sourcing

### 6. **Type Safety**
- Compile-time type checking via generics
- No runtime type casting errors
- IDE autocomplete for commands and queries

## Performance

### CommandBus
- **Handler Lookup**: O(1) - ConcurrentHashMap
- **Validation**: ~10-100µs (JSR-303)
- **Overhead**: <1ms per command
- **Monitoring**: Logs commands >1s execution time

### QueryBus
- **Handler Lookup**: O(1) - ConcurrentHashMap
- **Validation**: ~10-100µs (JSR-303)
- **Overhead**: <1ms per query
- **Monitoring**: Logs queries >500ms execution time

## Best Practices

### Commands
1. ✅ Use JSR-303 validation annotations
2. ✅ Return only essential data (ID, not full entity)
3. ✅ Publish domain events after state changes
4. ✅ Make handlers transactional (`@Transactional`)
5. ❌ Don't return full entities (use IDs or minimal DTOs)
6. ❌ Don't query data in commands (read-your-writes pattern)

### Queries
1. ✅ Use read-only transactions (`@Transactional(readOnly = true)`)
2. ✅ Return DTOs, not entities (avoid lazy loading)
3. ✅ Add caching where appropriate (`@Cacheable`)
4. ✅ Optimize database queries (projections, indexes)
5. ❌ Don't modify state in queries
6. ❌ Don't trigger side effects

### Handlers
1. ✅ One handler per command/query
2. ✅ Keep handlers focused (Single Responsibility)
3. ✅ Use constructor injection for dependencies
4. ✅ Log important operations
5. ✅ Handle errors gracefully (throw meaningful exceptions)

## Examples in Codebase

### Commands
- `CreateUserCommand` → `CreateUserCommandHandler`
  - Location: `iam-service/src/main/java/com/enterprise/iam/command/`
  - Creates new users with validation and event publishing

### Queries
- `GetUserByIdQuery` → `GetUserByIdQueryHandler`
  - Location: `iam-service/src/main/java/com/enterprise/iam/query/`
  - Fetches user with caching and DTO transformation

## Migration Guide

### Existing Code → CQRS

**Before (Traditional Service Layer):**
```java
@Service
public class UserService {
    public UUID createUser(String email, String firstName, String lastName) {
        // Validation + Business logic mixed
    }

    public UserDTO getUser(UUID id) {
        // Read logic
    }
}

@RestController
public class UserController {
    @Autowired
    private UserService userService; // Tight coupling

    @PostMapping("/users")
    public UUID createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request.email(), ...);
    }
}
```

**After (CQRS):**
```java
// No UserService needed! Handlers are the services.

@RestController
public class UserController {
    @Autowired
    private CommandBus commandBus; // Decoupled

    @Autowired
    private QueryBus queryBus; // Decoupled

    @PostMapping("/users")
    public UUID createUser(@RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(request.email(), ...);
        return commandBus.dispatch(command);
    }
}
```

## Troubleshooting

### Issue: "No handler registered for command"
**Cause**: Handler not found or not annotated with `@Service`

**Solution**:
1. Ensure handler implements `CommandHandler<C, R>`
2. Annotate handler class with `@Service`
3. Verify handler is in component scan package
4. Check application logs for CQRS registration messages

### Issue: "Command validation failed"
**Cause**: JSR-303 validation constraint violated

**Solution**:
1. Check command record fields for validation annotations
2. Ensure input data matches constraints
3. Review error message for specific field violations

### Issue: "Slow command execution warning"
**Cause**: Command took >1s to execute

**Solution**:
1. Check database query performance
2. Add database indexes if needed
3. Consider async processing for long operations
4. Review transaction boundaries

## Monitoring

### Startup Logs
```
🔧 Auto-registering CQRS handlers...
✅ Registered handler: CreateUserCommand -> CreateUserCommandHandler
✅ Registered handler: GetUserByIdQuery -> GetUserByIdQueryHandler
✅ CQRS initialization complete: 5 command handlers, 8 query handlers
```

### Runtime Logs
```
📤 Dispatching command: CreateUserCommand -> CreateUserCommandHandler
✅ Command executed: CreateUserCommand in 125ms

🔍 Dispatching query: GetUserByIdQuery -> GetUserByIdQueryHandler
✅ Query executed: GetUserByIdQuery in 45ms

⚠️ Slow command execution: CreateOrderCommand took 1250ms (>1s)
⚠️ Slow query execution: SearchUsersQuery took 750ms (>500ms)
```

## Future Enhancements

### Planned Features
- [ ] Async command processing (message queue integration)
- [ ] Event sourcing support
- [ ] CQRS metrics endpoint (`/actuator/cqrs`)
- [ ] Command/Query middleware pipeline
- [ ] Distributed tracing integration
- [ ] Command replay for debugging

## References

- **CQRS Pattern**: https://martinfowler.com/bliki/CQRS.html
- **Event Sourcing**: https://martinfowler.com/eaaDev/EventSourcing.html
- **Domain Events**: https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events

---

**Status**: ✅ Production Ready
**Version**: 1.0.0
**Last Updated**: 2025-12-31
