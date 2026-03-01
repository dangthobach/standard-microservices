package com.enterprise.common.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SoftDeletableEntity.
 * Tests soft delete, restore, and field tracking functionality.
 */
class SoftDeletableEntityTest {

    /**
     * Concrete implementation for testing the abstract class
     */
    static class TestSoftDeletableEntity extends SoftDeletableEntity<Long> {
        private static final long serialVersionUID = 1L;
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDeleteTests {

        @Test
        @DisplayName("should mark entity as deleted with user and timestamp")
        void shouldMarkEntityAsDeleted() {
            var entity = new TestSoftDeletableEntity();
            entity.setId(1L);

            entity.softDelete("admin@example.com");

            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedBy()).isEqualTo("admin@example.com");
            assertThat(entity.getDeletedAt()).isNotNull();
            assertThat(entity.getDeletedAt()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("should preserve deletedBy when soft-deleted multiple times")
        void shouldPreserveDeletedByOnMultipleCalls() {
            var entity = new TestSoftDeletableEntity();
            entity.softDelete("user1");
            entity.softDelete("user2");

            assertThat(entity.getDeletedBy()).isEqualTo("user2");
        }
    }

    @Nested
    @DisplayName("restore()")
    class RestoreTests {

        @Test
        @DisplayName("should restore a soft-deleted entity")
        void shouldRestoreSoftDeletedEntity() {
            var entity = new TestSoftDeletableEntity();
            entity.softDelete("admin");

            entity.restore();

            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getDeletedBy()).isNull();
            assertThat(entity.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("should be safe to call on non-deleted entity")
        void shouldBeSafeOnNonDeletedEntity() {
            var entity = new TestSoftDeletableEntity();

            assertThatCode(() -> entity.restore()).doesNotThrowAnyException();
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("isActive()")
    class IsActiveTests {

        @Test
        @DisplayName("new entity should be active")
        void newEntityShouldBeActive() {
            var entity = new TestSoftDeletableEntity();

            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("soft-deleted entity should not be active")
        void deletedEntityShouldNotBeActive() {
            var entity = new TestSoftDeletableEntity();
            entity.softDelete("admin");

            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("restored entity should be active again")
        void restoredEntityShouldBeActive() {
            var entity = new TestSoftDeletableEntity();
            entity.softDelete("admin");
            entity.restore();

            assertThat(entity.isActive()).isTrue();
        }
    }
}
