package com.enterprise.iam.entity;

import com.enterprise.common.entity.StatefulEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User Request Entity
 * <p>
 * Represents a user creation request in the Maker/Checker workflow.
 * Extends StatefulEntity to manage state transitions (DRAFT -> WAITING_FOR_APPROVAL -> APPROVED/REJECTED).
 * <p>
 * Features:
 * - State machine validation via StatefulEntity
 * - Tracks request creator (Maker) for separation of duties
 * - Stores user data (email, firstName, lastName, roles)
 * - Indexed for high-performance queries at 1M user scale
 * <p>
 * Separation of Duties:
 * - Maker (requestCreatorId) cannot approve their own request
 * - Only Checker (different user) can approve/reject
 */
@Entity
@Table(name = "user_requests", indexes = {
        @Index(name = "idx_request_status", columnList = "status"),
        @Index(name = "idx_request_creator", columnList = "request_creator_id"),
        @Index(name = "idx_request_email", columnList = "email"),
        @Index(name = "idx_request_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest extends StatefulEntity<UUID, UserRequestStatus> {

    /**
     * Email address for the new user
     */
    @Column(nullable = false, length = 100)
    private String email;

    /**
     * First name for the new user
     */
    @Column(nullable = false, length = 100)
    private String firstName;

    /**
     * Last name for the new user
     */
    @Column(nullable = false, length = 100)
    private String lastName;

    /**
     * Roles to be assigned to the new user
     * Stored as role IDs (UUIDs) in JSONB format for flexibility
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_request_roles", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "role_id")
    @Builder.Default
    private Set<UUID> roleIds = new HashSet<>();

    /**
     * ID of the user who created this request (Maker)
     * Used for separation of duties validation
     */
    @Column(name = "request_creator_id", nullable = false, length = 100)
    private String requestCreatorId;

    /**
     * Reason for rejection (populated when status is REJECTED)
     * Managed by StatefulEntity.statusChangeReason
     */

    /**
     * Initialize status to DRAFT if not set
     */
    @PostLoad
    @PrePersist
    protected void initializeStatus() {
        if (getStatus() == null) {
            setStatus(UserRequestStatus.DRAFT);
        }
    }

    /**
     * Validate state transitions
     * <p>
     * Allowed transitions:
     * - DRAFT -> WAITING_FOR_APPROVAL (Submit)
     * - WAITING_FOR_APPROVAL -> APPROVED (Approve)
     * - WAITING_FOR_APPROVAL -> REJECTED (Reject)
     * - REJECTED -> WAITING_FOR_APPROVAL (Resubmit)
     */
    @Override
    protected boolean canTransitionTo(UserRequestStatus newStatus) {
        if (getStatus() == null) {
            // Initial state - can only be DRAFT
            return newStatus == UserRequestStatus.DRAFT;
        }

        return switch (getStatus()) {
            case DRAFT -> newStatus == UserRequestStatus.WAITING_FOR_APPROVAL;
            case WAITING_FOR_APPROVAL -> newStatus == UserRequestStatus.APPROVED ||
                    newStatus == UserRequestStatus.REJECTED;
            case REJECTED -> newStatus == UserRequestStatus.WAITING_FOR_APPROVAL;
            case APPROVED -> false; // Final state - cannot transition from APPROVED
        };
    }

    /**
     * Get full name (computed property)
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

