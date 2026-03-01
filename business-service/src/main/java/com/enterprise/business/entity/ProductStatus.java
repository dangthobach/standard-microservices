package com.enterprise.business.entity;

/**
 * Product Status Enum
 *
 * State Machine:
 * - DRAFT -> PENDING_APPROVAL (Submit for review)
 * - PENDING_APPROVAL -> APPROVED (Approve)
 * - PENDING_APPROVAL -> REJECTED (Reject)
 * - APPROVED -> ACTIVE (Activate / Confirm)
 * - REJECTED -> DRAFT (Revise and resubmit)
 * - ACTIVE -> INACTIVE (Deactivate)
 * - INACTIVE -> ACTIVE (Reactivate)
 */
public enum ProductStatus {
    /** Initial state - product is being created/edited */
    DRAFT,

    /** Product submitted for approval */
    PENDING_APPROVAL,

    /** Product approved but not yet active */
    APPROVED,

    /** Product rejected - can be revised */
    REJECTED,

    /** Product is live and available */
    ACTIVE,

    /** Product is temporarily deactivated */
    INACTIVE
}
