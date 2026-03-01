package com.enterprise.common.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StatefulEntity.
 * Tests state transitions, validation, and status tracking.
 */
class StatefulEntityTest {

    enum TestStatus {
        DRAFT, SUBMITTED, APPROVED, REJECTED
    }

    /**
     * Concrete implementation for testing
     */
    static class TestStatefulEntity extends StatefulEntity<Long, TestStatus> {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean canTransitionTo(TestStatus newStatus) {
            if (getStatus() == null) {
                return newStatus == TestStatus.DRAFT;
            }
            return switch (getStatus()) {
                case DRAFT -> newStatus == TestStatus.SUBMITTED;
                case SUBMITTED -> newStatus == TestStatus.APPROVED || newStatus == TestStatus.REJECTED;
                case REJECTED -> newStatus == TestStatus.DRAFT;
                case APPROVED -> false; // terminal state
            };
        }
    }

    private TestStatefulEntity createEntity(TestStatus initialStatus) {
        var entity = new TestStatefulEntity();
        entity.setId(1L);
        entity.setStatus(initialStatus);
        return entity;
    }

    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatusTests {

        @Test
        @DisplayName("should transition DRAFT -> SUBMITTED successfully")
        void shouldTransitionDraftToSubmitted() {
            var entity = createEntity(TestStatus.DRAFT);

            entity.changeStatus(TestStatus.SUBMITTED, "maker", "Ready for review");

            assertThat(entity.getStatus()).isEqualTo(TestStatus.SUBMITTED);
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.DRAFT);
            assertThat(entity.getStatusChangedBy()).isEqualTo("maker");
            assertThat(entity.getStatusChangeReason()).isEqualTo("Ready for review");
            assertThat(entity.getStatusChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition SUBMITTED -> APPROVED successfully")
        void shouldTransitionSubmittedToApproved() {
            var entity = createEntity(TestStatus.SUBMITTED);

            entity.changeStatus(TestStatus.APPROVED, "checker", "Looks good");

            assertThat(entity.getStatus()).isEqualTo(TestStatus.APPROVED);
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.SUBMITTED);
        }

        @Test
        @DisplayName("should transition SUBMITTED -> REJECTED successfully")
        void shouldTransitionSubmittedToRejected() {
            var entity = createEntity(TestStatus.SUBMITTED);

            entity.changeStatus(TestStatus.REJECTED, "checker", "Missing info");

            assertThat(entity.getStatus()).isEqualTo(TestStatus.REJECTED);
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.SUBMITTED);
        }

        @Test
        @DisplayName("should transition REJECTED -> DRAFT for re-submission")
        void shouldTransitionRejectedToDraft() {
            var entity = createEntity(TestStatus.REJECTED);

            entity.changeStatus(TestStatus.DRAFT, "maker", "Revised and ready");

            assertThat(entity.getStatus()).isEqualTo(TestStatus.DRAFT);
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.REJECTED);
        }

        @Test
        @DisplayName("should throw on invalid transition from terminal state")
        void shouldThrowOnInvalidTransitionFromTerminal() {
            var entity = createEntity(TestStatus.APPROVED);

            assertThatThrownBy(() -> entity.changeStatus(TestStatus.DRAFT, "user", "want to change"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("should throw on invalid transition DRAFT -> APPROVED")
        void shouldThrowOnInvalidDraftToApproved() {
            var entity = createEntity(TestStatus.DRAFT);

            assertThatThrownBy(() -> entity.changeStatus(TestStatus.APPROVED, "user", "shortcut"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should no-op when changing to same status")
        void shouldNoOpWhenChangingToSameStatus() {
            var entity = createEntity(TestStatus.DRAFT);

            // Implementation silently returns — no exception
            assertThatCode(() -> entity.changeStatus(TestStatus.DRAFT, "user", "same"))
                    .doesNotThrowAnyException();
            assertThat(entity.getStatus()).isEqualTo(TestStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("previousStatus tracking")
    class PreviousStatusTests {

        @Test
        @DisplayName("should track full status history chain")
        void shouldTrackStatusHistoryChain() {
            var entity = createEntity(TestStatus.DRAFT);

            entity.changeStatus(TestStatus.SUBMITTED, "maker", null);
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.DRAFT);

            entity.changeStatus(TestStatus.REJECTED, "checker", "Missing field");
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.SUBMITTED);

            entity.changeStatus(TestStatus.DRAFT, "maker", "Fixed");
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.REJECTED);

            entity.changeStatus(TestStatus.SUBMITTED, "maker", "Re-submitted");
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.DRAFT);

            entity.changeStatus(TestStatus.APPROVED, "checker", "All good");
            assertThat(entity.getPreviousStatus()).isEqualTo(TestStatus.SUBMITTED);
            assertThat(entity.getStatus()).isEqualTo(TestStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("new entity should have null status")
        void newEntityShouldHaveNullStatus() {
            var entity = new TestStatefulEntity();
            assertThat(entity.getStatus()).isNull();
            assertThat(entity.getPreviousStatus()).isNull();
        }

        @Test
        @DisplayName("changedBy and reason should be recorded")
        void shouldRecordMetadata() {
            var entity = createEntity(TestStatus.DRAFT);

            entity.changeStatus(TestStatus.SUBMITTED, "john.doe@example.com", "Ready for approval");

            assertThat(entity.getStatusChangedBy()).isEqualTo("john.doe@example.com");
            assertThat(entity.getStatusChangeReason()).isEqualTo("Ready for approval");
        }
    }
}
