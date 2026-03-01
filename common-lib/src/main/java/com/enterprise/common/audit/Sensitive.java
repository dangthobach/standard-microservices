package com.enterprise.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for fields that contain sensitive data.
 * <p>
 * Used by SensitiveDataMasker to mask PII (Email, Phone, etc.) in logs and
 * audit trails.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    /**
     * Type of sensitivity for masking strategy.
     */
    Sensitivity type() default Sensitivity.PARTIAL;

    enum Sensitivity {
        NONE, // No masking
        PARTIAL, // Mask parts (e.g., j***@example.com)
        FULL // Mask completely (e.g., ******)
    }
}
