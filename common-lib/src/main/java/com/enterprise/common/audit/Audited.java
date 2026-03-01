package com.enterprise.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that should be audited.
 * <p>
 * Aspect will intercept execution and publish an AuditEvent.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * Action name. If empty, method name is used.
     */
    String action() default "";

    /**
     * Entity Type (e.g., USER, PRODUCT).
     */
    String entityType() default "";

    /**
     * Field name to extract ID from result object.
     */
    String entityIdField() default "";
}
