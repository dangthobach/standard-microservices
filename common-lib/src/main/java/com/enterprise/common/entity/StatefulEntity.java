package com.enterprise.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateful Entity with State Machine Support
 *
 * Features:
 * - Track current state
 * - Track state transitions history
 * - Validation before state change
 * - Event publishing on state change
 *
 * Usage:
 * <pre>
 * @Entity
 * public class Order extends StatefulEntity<Long, OrderStatus> {
 *
 *     @Override
 *     protected boolean canTransitionTo(OrderStatus newStatus) {
 *         return switch (getStatus()) {
 *             case DRAFT -> newStatus == OrderStatus.SUBMITTED;
 *             case SUBMITTED -> newStatus == OrderStatus.APPROVED || newStatus == OrderStatus.REJECTED;
 *             case APPROVED -> newStatus == OrderStatus.COMPLETED;
 *             default -> false;
 *         };
 *     }
 * }
 * </pre>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class StatefulEntity<ID extends Serializable, S extends Enum<S>> 
        extends SoftDeletableEntity<ID> {

    /**
     * Current status of the entity
     */
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private S status;

    /**
     * Previous status before current one
     */
    @Column(name = "previous_status", length = 50)
    @Enumerated(EnumType.STRING)
    private S previousStatus;

    /**
     * When status was last changed
     */
    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    /**
     * Who changed the status
     */
    @Column(name = "status_changed_by", length = 100)
    private String statusChangedBy;

    /**
     * Reason/comment for status change
     */
    @Column(name = "status_change_reason", length = 500)
    private String statusChangeReason;

    /**
     * Change status with validation
     *
     * @param newStatus New status to transition to
     * @param changedBy User performing the change
     * @param reason Reason for the change
     * @throws IllegalStateException if transition is not allowed
     */
    public void changeStatus(S newStatus, String changedBy, String reason) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        if (newStatus.equals(this.status)) {
            return; // No change needed
        }

        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.status, newStatus)
            );
        }

        this.previousStatus = this.status;
        this.status = newStatus;
        this.statusChangedAt = Instant.now();
        this.statusChangedBy = changedBy;
        this.statusChangeReason = reason;

        onStatusChanged(previousStatus, newStatus);
    }

    /**
     * Hook method called after status change
     * Override to add custom logic (e.g., publish events)
     */
    protected void onStatusChanged(S oldStatus, S newStatus) {
        // Default: do nothing
        // Override in subclass to add custom logic
    }

    /**
     * Validate if transition to new status is allowed
     * Must be implemented by subclass
     *
     * @param newStatus Target status
     * @return true if transition is allowed
     */
    protected abstract boolean canTransitionTo(S newStatus);

    /**
     * Check if entity is in specific status
     */
    public boolean hasStatus(S targetStatus) {
        return this.status == targetStatus;
    }

    /**
     * Check if entity is in any of the given statuses
     */
    @SafeVarargs
    public final boolean hasAnyStatus(S... statuses) {
        for (S s : statuses) {
            if (this.status == s) {
                return true;
            }
        }
        return false;
    }
}
