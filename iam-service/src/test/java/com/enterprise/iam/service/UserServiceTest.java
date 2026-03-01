package com.enterprise.iam.service;

import com.enterprise.iam.entity.User;
import com.enterprise.iam.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Uses Mockito to mock UserRepository — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User createTestUser(UUID id, String email) {
        return User.builder()
                .keycloakId("kc-" + id)
                .email(email)
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            var id = UUID.randomUUID();
            var user = createTestUser(id, "john@example.com");
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            var result = userService.findById(id);

            assertThat(result).isPresent().contains(user);
            verify(userRepository).findById(id);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            var id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            var result = userService.findById(id);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmailTests {

        @Test
        @DisplayName("should find user by email")
        void shouldFindByEmail() {
            var user = createTestUser(UUID.randomUUID(), "john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

            var result = userService.findByEmail("john@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should return empty for unknown email")
        void shouldReturnEmptyForUnknown() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            var result = userService.findByEmail("unknown@example.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByKeycloakId()")
    class FindByKeycloakIdTests {

        @Test
        @DisplayName("should find user by keycloak ID")
        void shouldFindByKeycloakId() {
            var user = createTestUser(UUID.randomUUID(), "john@example.com");
            when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

            var result = userService.findByKeycloakId("kc-123");

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should save and return user")
        void shouldSaveAndReturnUser() {
            var user = createTestUser(UUID.randomUUID(), "john@example.com");
            when(userRepository.save(user)).thenReturn(user);

            var result = userService.save(user);

            assertThat(result).isEqualTo(user);
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteByIdTests {

        @Test
        @DisplayName("should soft-delete user when found")
        void shouldSoftDeleteUser() {
            var id = UUID.randomUUID();
            var user = createTestUser(id, "john@example.com");
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.deleteById(id);

            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedBy()).isEqualTo("system");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should do nothing when user not found")
        void shouldDoNothingWhenNotFound() {
            var id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            userService.deleteById(id);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmailTests {

        @Test
        @DisplayName("should return true when active user exists")
        void shouldReturnTrueWhenExists() {
            when(userRepository.existsByEmailAndDeletedFalse("john@example.com")).thenReturn(true);

            assertThat(userService.existsByEmail("john@example.com")).isTrue();
        }

        @Test
        @DisplayName("should return false when no active user")
        void shouldReturnFalseWhenNotExists() {
            when(userRepository.existsByEmailAndDeletedFalse("john@example.com")).thenReturn(false);

            assertThat(userService.existsByEmail("john@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByKeycloakId()")
    class ExistsByKeycloakIdTests {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            when(userRepository.existsByKeycloakIdAndDeletedFalse("kc-123")).thenReturn(true);

            assertThat(userService.existsByKeycloakId("kc-123")).isTrue();
            verify(userRepository).existsByKeycloakIdAndDeletedFalse("kc-123");
        }
    }

    @Nested
    @DisplayName("countEnabledUsers()")
    class CountEnabledUsersTests {

        @Test
        @DisplayName("should return count of enabled users")
        void shouldReturnCount() {
            when(userRepository.countByEnabled(true)).thenReturn(42L);

            assertThat(userService.countEnabledUsers()).isEqualTo(42L);
        }
    }
}
