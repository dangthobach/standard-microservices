package com.enterprise.iam.entity;

/**
 * User Request Action Enum
 * <p>
 * Represents the action performed on a user request for audit logging.
 */
public enum UserRequestAction {
    /**
     * Request was created
     */
    CREATE,

    /**
     * Request was updated (edited)
     */
    UPDATE,

    /**
     * Request was submitted for approval
     */
    SUBMIT,

    /**
     * Request was approved by Checker
     */
    APPROVE,

    /**
     * Request was rejected by Checker
     */
    REJECT
}

