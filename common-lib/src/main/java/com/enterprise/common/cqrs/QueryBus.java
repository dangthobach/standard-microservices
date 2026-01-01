package com.enterprise.common.cqrs;

/**
 * Query Bus Interface
 *
 * Central dispatcher for all queries in the system.
 * Responsible for:
 * - Finding the correct QueryHandler for a given Query
 * - Executing the handler
 * - Applying caching and optimization
 *
 * Architecture:
 * <pre>
 * Controller
 *   ↓
 * queryBus.dispatch(GetUserByIdQuery)
 *   ↓
 * [Caching Layer - Check L1/L2 Cache]
 *   ↓
 * GetUserByIdQueryHandler.handle(query)
 *   ↓
 * [Transform to DTO]
 *   ↓
 * Return Result
 * </pre>
 *
 * Benefits:
 * - Decouples controllers from handlers
 * - Centralized caching strategy
 * - Easier to optimize read performance
 * - Clear read-only semantics
 *
 * Usage in Controller:
 * <pre>
 * @RestController
 * @RequiredArgsConstructor
 * public class UserController {
 *
 *     private final QueryBus queryBus;
 *
 *     @GetMapping("/users/{id}")
 *     public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID id) {
 *         GetUserByIdQuery query = new GetUserByIdQuery(id);
 *         UserDTO user = queryBus.dispatch(query);
 *         return ResponseEntity.ok(ApiResponse.success(user));
 *     }
 *
 *     @GetMapping("/users")
 *     public ResponseEntity<ApiResponse<Page<UserDTO>>> searchUsers(
 *         @RequestParam String keyword,
 *         Pageable pageable
 *     ) {
 *         SearchUsersQuery query = new SearchUsersQuery(keyword, pageable);
 *         Page<UserDTO> users = queryBus.dispatch(query);
 *         return ResponseEntity.ok(ApiResponse.success(users));
 *     }
 * }
 * </pre>
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
public interface QueryBus {

    /**
     * Dispatch a query to its handler
     *
     * Flow:
     * 1. Find QueryHandler<Q, R> for the given query type
     * 2. Check cache if handler is annotated with @Cacheable
     * 3. Execute handler.handle(query)
     * 4. Return result
     *
     * @param query The query to dispatch
     * @param <Q>   The query type
     * @param <R>   The result type
     * @return The result of executing the query
     * @throws IllegalArgumentException if no handler found for query
     */
    <Q extends Query<R>, R> R dispatch(Q query);
}
