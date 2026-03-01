package com.enterprise.common.exception;

import lombok.Getter;

/**
 * Thrown when an invalid state transition is attempted on a stateful entity.
 * Automatically handled by GlobalExceptionHandler → 422 UNPROCESSABLE_ENTITY.
 */
@Getter
public class StateTransitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String entityType;
    private final String currentState;
    private final String targetState;

    public StateTransitionException(String entityType, String currentState, String targetState) {
        super(String.format("Cannot transition %s from %s to %s", entityType, currentState, targetState));
        this.entityType = entityType;
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public StateTransitionException(String entityType, String currentState, String targetState, String reason) {
        super(String.format("Cannot transition %s from %s to %s: %s", entityType, currentState, targetState, reason));
        this.entityType = entityType;
        this.currentState = currentState;
        this.targetState = targetState;
    }
}
