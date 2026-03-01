package com.enterprise.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic State History Entity
 *
 * Records all state transitions for any StatefulEntity.
 * Provides a complete, immutable audit trail of status changes.
 *
 * Fields:
 * - entityType: Fully qualified class name or simple name of the entity
 * - entityId: UUID of the entity that changed state
 * - fromStatus: Previous status (null for initial state)
 * - toStatus: New status after transition
 * - changedBy: User who triggered the change
 * - changedAt: Timestamp of the change
 * - reason: Optional reason/comment for the change
 * - metadata: Optional JSON metadata (e.g., snapshot of changed fields)
 *
 * Usage:
 * 
 * <pre>
 * // Automatically recorded when using StatefulEntity.changeStatus()
 * // Or manually create:
 * StateHistory history = StateHistory.builder()
 *         .entityType("Product")
 *         .entityId(product.getId())
 *         .fromStatus("DRAFT")
 *         .toStatus("PENDING_APPROVAL")
 *         .changedBy("john.doe")
 *         .reason("Ready for review")
 *         .build();
 * stateHistoryRepository.save(history);
 * </pre>
 */
@Entity
@Table(name = "state_history", indexes = {
        @Index(name = "idx_sh_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_sh_entity_id", columnList = "entity_id"),
        @Index(name = "idx_sh_changed_at", columnList = "changed_at"),
        @Index(name = "idx_sh_changed_by", columnList = "changed_by")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Type of entity (e.g., "Product", "UserRequest", "Order")
     */
    @Column(name = "entity_type", nullable = false, length = 100, updatable = false)
    private String entityType;

    /**
     * ID of the entity (stored as String for flexibility across ID types)
     */
    @Column(name = "entity_id", nullable = false, length = 100, updatable = false)
    private String entityId;

    /**
     * Previous status (null for initial state)
     */
    @Column(name = "from_status", length = 50, updatable = false)
    private String fromStatus;

    /**
     * New status after transition
     */
    @Column(name = "to_status", nullable = false, length = 50, updatable = false)
    private String toStatus;

    /**
     * User who triggered the state change
     */
    @CreatedBy
    @Column(name = "changed_by", nullable = false, length = 100, updatable = false)
    private String changedBy;

    /**
     * When the state change occurred
     */
    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    /**
     * Reason or comment for the state change
     */
    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    /**
     * Optional JSON metadata (e.g., field change snapshot)
     */
    @Column(name = "metadata", columnDefinition = "TEXT", updatable = false)
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
