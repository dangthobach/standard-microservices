package com.enterprise.common.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BaseEntity.
 * Tests ID management, equality, and hashCode behavior.
 */
class BaseEntityTest {

    static class TestBaseEntity extends BaseEntity<Long> {
        private static final long serialVersionUID = 1L;
    }

    static class TestUuidEntity extends BaseEntity<UUID> {
        private static final long serialVersionUID = 1L;
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("entities with same ID should be equal")
        void entitiesWithSameIdShouldBeEqual() {
            var e1 = new TestBaseEntity();
            e1.setId(1L);
            var e2 = new TestBaseEntity();
            e2.setId(1L);

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }

        @Test
        @DisplayName("entities with different IDs should not be equal")
        void entitiesWithDifferentIdsShouldNotBeEqual() {
            var e1 = new TestBaseEntity();
            e1.setId(1L);
            var e2 = new TestBaseEntity();
            e2.setId(2L);

            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("entity should not equal null")
        void entityShouldNotEqualNull() {
            var entity = new TestBaseEntity();
            entity.setId(1L);

            assertThat(entity).isNotEqualTo(null);
        }

        @Test
        @DisplayName("UUID entities with same UUID should be equal")
        void uuidEntitiesShouldBeEqual() {
            var uuid = UUID.randomUUID();
            var e1 = new TestUuidEntity();
            e1.setId(uuid);
            var e2 = new TestUuidEntity();
            e2.setId(uuid);

            assertThat(e1).isEqualTo(e2);
        }
    }

    @Nested
    @DisplayName("Persistable behavior")
    class PersistableTests {

        @Test
        @DisplayName("new entity should be marked as new")
        void newEntityShouldBeNew() {
            var entity = new TestBaseEntity();
            assertThat(entity.isNew()).isTrue();
        }

        @Test
        @DisplayName("entity with ID should not be new")
        void entityWithIdShouldNotBeNew() {
            var entity = new TestBaseEntity();
            entity.setId(1L);
            assertThat(entity.isNew()).isFalse();
        }
    }
}
