package com.enterprise.business.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity for entities that need state tracking and history
 *
 * IMPORTANT: History is NOT saved automatically via JPA callbacks.
 * Use BaseStatefulService.saveWithHistory() to save entity + history in same
 * transaction.
 *
 * Generic Type Parameter:
 * - S: Status enum type (e.g., WarehouseStatus, ContractStatus)
 *
 * State Machine:
 * - Override canTransitionTo() to define valid state transitions
 * - Use transitionTo() instead of setStatus() to enforce validation
 *
 * Usage:
 * 1. Extend this class: public class Warehouse extends
 * StatefulEntity<WarehouseStatus>
 * 2. Create corresponding History entity extending HistoryEntity
 * 3. Override createHistorySnapshot() to create history record with full
 * context
 * 4. Override canTransitionTo() to define valid state transitions
 * 5. Use BaseStatefulService for all save operations
 */
@Getter
@Setter
@MappedSuperclass
public abstract class StatefulEntity<S extends Enum<S>, H extends HistoryEntity> extends SoftDeleteEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private S status;

    /**
     * Define valid transitions. Override này là BẮT BUỘC về mặt design.
     * Default throw để buộc developer phải suy nghĩ về state machine.
     */
    public boolean canTransitionTo(S newStatus) {
        throw new UnsupportedOperationException(
                "canTransitionTo() must be overridden in " + getClass().getSimpleName() +
                        " to define valid state transitions");
    }

    /**
     * Transition to new status with validation
     * Throws exception if transition is not allowed
     *
     * @param newStatus Target status
     * @throws IllegalStateException if transition is invalid
     */
    public void transitionTo(S newStatus) {
        S previousStatus = this.status;
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("[%s] Invalid transition: %s -> %s",
                            getEntityType(), previousStatus, newStatus));
        }
        this.status = newStatus;
    }

    /**
     * Tự động từ class name, không cần override ở subclass nữa
     * Override nếu cần custom name
     */
    public String getEntityType() {
        return this.getClass().getSimpleName()
                .replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toUpperCase();
    }

    /**
     * Create a history snapshot of current entity state
     * Must be implemented by concrete entities
     *
     * @param action         Action type (CREATE, UPDATE, DELETE, RESTORE)
     * @param previousStatus Previous status before change
     * @param changedBy      User who made the change
     * @param snapshot       JSON snapshot of entity state (DTO, not raw entity)
     * @param diff           Diff of what changed
     * @param correlationId  Correlation ID for distributed tracing
     * @return History entity representing current state
     */
    public abstract H createHistorySnapshot(
            String action,
            S previousStatus,
            String changedBy,
            String snapshot,
            String diff,
            String correlationId);
}
