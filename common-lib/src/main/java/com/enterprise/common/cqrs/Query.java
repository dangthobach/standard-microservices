package com.enterprise.common.cqrs;

/**
 * Base marker interface for all Queries
 *
 * Queries represent read operations (GET, SEARCH) that do not change system state.
 *
 * CQRS Pattern Benefits:
 * - Clear separation between reads (Query) and writes (Command)
 * - Queries can use optimized read models or caching
 * - Enables separate read replicas for scalability
 * - Simpler query logic without side effects
 *
 * Query Best Practices:
 * - Queries should be read-only (no state changes)
 * - Can read from cache or read replicas
 * - Should not trigger side effects
 * - Can return DTOs optimized for the view
 *
 * Usage:
 * <pre>
 * public record GetUserByIdQuery(
 *     UUID userId
 * ) implements Query<UserDTO> {}
 *
 * public record SearchUsersQuery(
 *     String keyword,
 *     Pageable pageable
 * ) implements Query<Page<UserDTO>> {}
 *
 * @Service
 * public class GetUserByIdQueryHandler implements QueryHandler<GetUserByIdQuery, UserDTO> {
 *     private final UserRepository userRepository;
 *
 *     @Override
 *     @Cacheable("users")
 *     public UserDTO handle(GetUserByIdQuery query) {
 *         User user = userRepository.findById(query.userId())
 *             .orElseThrow(() -> new UserNotFoundException(query.userId()));
 *
 *         return UserDTO.from(user);
 *     }
 * }
 * </pre>
 *
 * @param <R> The type of result returned by the query
 */
public interface Query<R> {
    // Marker interface - no methods
    // The generic type R represents the query result (e.g., UserDTO, Page<UserDTO>)
}
