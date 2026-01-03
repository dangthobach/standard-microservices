package com.enterprise.iam.entity;

import com.enterprise.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * User Request History Entity
 * <p>
 * Audit log for all state changes and actions on UserRequest entities.
 * Extends AuditableEntity for automatic tracking of who and when.
 * <p>
 * Purpose:
 * - Fast history reads at 1M user scale (dedicated table instead of Envers)
 * - Complete audit trail for compliance
 * - Track all actions: CREATE, UPDATE, SUBMIT, APPROVE, REJECT
 * <p>
 * Performance:
 * - Indexed on request_id for fast lookups
 * - Indexed on action for filtering
 * - JSONB metadata for flexible field tracking
 */
@Entity
@Table(name = "user_request_history", indexes = {
        @Index(name = "idx_history_request_id", columnList = "request_id"),
        @Index(name = "idx_history_action", columnList = "action"),
        @Index(name = "idx_history_actor_id", columnList = "actor_id"),
        @Index(name = "idx_history_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestHistory extends AuditableEntity<UUID> {

    /**
     * Reference to the UserRequest
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private UserRequest request;

    /**
     * Previous status before the change
     */
    @Column(name = "old_status", length = 50)
    @Enumerated(EnumType.STRING)
    private UserRequestStatus oldStatus;

    /**
     * New status after the change
     */
    @Column(name = "new_status", length = 50)
    @Enumerated(EnumType.STRING)
    private UserRequestStatus newStatus;

    /**
     * Action performed (CREATE, UPDATE, SUBMIT, APPROVE, REJECT)
     */
    @Column(name = "action", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private UserRequestAction action;

    /**
     * User ID who performed the action
     */
    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    /**
     * Comment/reason for the action
     * Required for REJECT actions
     */
    @Column(name = "comment", length = 500)
    private String comment;

    /**
     * JSON metadata snapshot of changed fields (if needed)
     * Example: {"email": "old@example.com -> new@example.com", "roles": ["ROLE_USER"]}
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}

