package com.enterprise.common.exception;

import lombok.Getter;

/**
 * Thrown when a requested resource cannot be found.
 * Automatically handled by GlobalExceptionHandler → 404 NOT_FOUND.
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String resourceType;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceType, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceType, fieldName, fieldValue));
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        this(resourceType, "id", id);
    }
}
