package com.enterprise.iam.entity;

/**
 * User Request Status Enum
 * <p>
 * Represents the state of a user creation request in the Maker/Checker workflow.
 * <p>
 * State Machine:
 * - DRAFT -> WAITING_FOR_APPROVAL (Submit)
 * - WAITING_FOR_APPROVAL -> APPROVED (Approve)
 * - WAITING_FOR_APPROVAL -> REJECTED (Reject)
 * - REJECTED -> WAITING_FOR_APPROVAL (Resubmit after edit)
 */
public enum UserRequestStatus {
    /**
     * Initial state - Request is being created/edited by Maker
     */
    DRAFT,

    /**
     * Request submitted and waiting for Checker approval
     */
    WAITING_FOR_APPROVAL,

    /**
     * Request approved by Checker - User will be created
     */
    APPROVED,

    /**
     * Request rejected by Checker - Can be edited and resubmitted
     */
    REJECTED
}

