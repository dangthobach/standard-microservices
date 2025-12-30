package com.enterprise.common.cqrs;

/**
 * Interface for handling Queries
 *
 * Implements the Query pattern for read operations.
 * Each QueryHandler is responsible for:
 * - Fetching data from repository or cache
 * - Transforming to DTO if needed
 * - Returning read-only results
 *
 * Best Practices:
 * - One handler per query type
 * - Handler should be read-only (@Transactional(readOnly = true))
 * - Use caching where appropriate (@Cacheable)
 * - Return DTOs, not entities (avoid lazy loading issues)
 * - Can use read replicas for better scalability
 *
 * Usage:
 * <pre>
 * @Service
 * @Transactional(readOnly = true)
 * public class GetUserByIdQueryHandler implements QueryHandler<GetUserByIdQuery, UserDTO> {
 *
 *     private final UserRepository userRepository;
 *
 *     @Override
 *     @Cacheable(value = "users", key = "#query.userId")
 *     public UserDTO handle(GetUserByIdQuery query) {
 *         User user = userRepository.findById(query.userId())
 *             .filter(User::isActive) // Only return non-deleted users
 *             .orElseThrow(() -> new UserNotFoundException(query.userId()));
 *
 *         return UserDTO.builder()
 *             .id(user.getId())
 *             .email(user.getEmail())
 *             .fullName(user.getFullName())
 *             .createdAt(user.getCreatedAt())
 *             .build();
 *     }
 * }
 *
 * @Service
 * @Transactional(readOnly = true)
 * public class SearchUsersQueryHandler implements QueryHandler<SearchUsersQuery, Page<UserDTO>> {
 *
 *     private final UserRepository userRepository;
 *
 *     @Override
 *     public Page<UserDTO> handle(SearchUsersQuery query) {
 *         return userRepository
 *             .findByEmailContainingIgnoreCaseAndDeletedFalse(
 *                 query.keyword(),
 *                 query.pageable()
 *             )
 *             .map(UserDTO::from);
 *     }
 * }
 * </pre>
 *
 * @param <Q> The query type
 * @param <R> The result type
 */
public interface QueryHandler<Q extends Query<R>, R> {

    /**
     * Handle the query and return result
     *
     * @param query The query to handle
     * @return The result of executing the query
     */
    R handle(Q query);
}
