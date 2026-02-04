package com.enterprise.business.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited
 * 
 * Usage:
 * @Audited(action = "CREATE_PRODUCT", entityType = "PRODUCT", entityIdField = "id")
 * public ProductDTO createProduct(CreateProductRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    
    /**
     * Action being performed (e.g., CREATE_PRODUCT, UPDATE_PRODUCT, DELETE_PRODUCT)
     */
    String action();
    
    /**
     * Type of entity being operated on (e.g., PRODUCT, USER, ORDER)
     */
    String entityType();
    
    /**
     * Field name in the return object that contains the entity ID
     * If empty, entity ID will not be extracted
     */
    String entityIdField() default "id";
}
