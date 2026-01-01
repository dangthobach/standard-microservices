package com.enterprise.iam.query;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.iam.dto.UserDTO;
import com.enterprise.iam.entity.User;
import com.enterprise.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Get User By ID Query Handler
 *
 * Handles the GetUserByIdQuery by:
 * 1. Fetching the user from repository
 * 2. Checking if user is active (not soft-deleted)
 * 3. Transforming to UserDTO
 * 4. Returning the DTO
 *
 * Caching:
 * - @Cacheable stores result in L1 cache (Caffeine)
 * - Key: "users::#{query.userId}"
 * - TTL: Configured in CacheConfiguration (typically 60s)
 * - Cache is invalidated on user update/delete
 *
 * Transaction Management:
 * - @Transactional(readOnly = true) for performance
 * - Optimizes database operations for read-only access
 *
 * @author Enterprise Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserByIdQueryHandler implements QueryHandler<GetUserByIdQuery, UserDTO> {

    private final UserRepository userRepository;

    /**
     * Handle GetUserByIdQuery
     *
     * @param query The query containing user ID
     * @return UserDTO
     * @throws IllegalArgumentException if user not found or is deleted
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#query.userId")
    public UserDTO handle(GetUserByIdQuery query) {
        log.debug("Handling GetUserByIdQuery: userId={}", query.userId());

        User user = userRepository.findById(query.userId())
            .filter(User::isActive) // Only return non-deleted users
            .orElseThrow(() -> new IllegalArgumentException(
                "User not found with ID: " + query.userId()
            ));

        log.debug("✅ User found: id={}, email={}", user.getId(), user.getEmail());

        return mapToDTO(user);
    }

    /**
     * Map User entity to UserDTO
     *
     * Transforms internal entity to external DTO representation.
     *
     * @param user The User entity
     * @return UserDTO
     */
    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .enabled(user.getEnabled())
            .emailVerified(user.getEmailVerified())
            .roles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()))
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
