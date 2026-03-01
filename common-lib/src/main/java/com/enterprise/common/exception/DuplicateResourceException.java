package com.enterprise.common.exception;

import lombok.Getter;

/**
 * Thrown when attempting to create a resource that already exists.
 * Automatically handled by GlobalExceptionHandler → 409 CONFLICT.
 */
@Getter
public class DuplicateResourceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String resourceType;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String resourceType, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceType, fieldName, fieldValue));
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
