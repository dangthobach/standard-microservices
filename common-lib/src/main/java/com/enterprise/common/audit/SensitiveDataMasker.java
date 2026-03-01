package com.enterprise.common.audit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Utility to mask sensitive data in objects using @Sensitive annotation.
 */
@Component
public class SensitiveDataMasker {

    public String mask(String value, Sensitive.Sensitivity type) {
        if (value == null || type == Sensitive.Sensitivity.NONE) {
            return value;
        }

        if (type == Sensitive.Sensitivity.FULL) {
            return "******";
        }

        // PARTIAL MASKING
        if (value.contains("@")) {
            // Email masking: j***@example.com
            String[] parts = value.split("@");
            if (parts.length == 2 && parts[0].length() > 1) {
                return parts[0].charAt(0) + "***@" + parts[1];
            }
        } else if (value.length() > 4) {
            // General masking: show last 4 chars
            return "***" + value.substring(value.length() - 4);
        }

        return "******";
    }

    /**
     * Create a masked copy of an object (JSON string representation).
     * Uses reflection to mask fields annotated with @Sensitive.
     */
    public Object maskObject(Object obj) {
        if (obj == null)
            return null;

        // For simple types, return as is
        if (isSimpleType(obj.getClass())) {
            return obj;
        }

        try {
            // We'll return a String representation (JSON-like) with masked values
            // because deep cloning mixed with reflection masking is very expensive/complex
            // for a runtime aspect.
            StringBuilder sb = new StringBuilder();
            sb.append(obj.getClass().getSimpleName()).append("{");

            Field[] fields = obj.getClass().getDeclaredFields();
            boolean first = true;
            for (Field field : fields) {
                field.setAccessible(true);
                if (!first)
                    sb.append(", ");
                first = false;

                sb.append(field.getName()).append("=");

                Object value = field.get(obj);
                if (field.isAnnotationPresent(Sensitive.class)) {
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    sb.append(mask(value != null ? value.toString() : "null", sensitive.type()));
                } else {
                    sb.append(value);
                }
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return obj.toString(); // Fallback
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(String.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Boolean.class) ||
                clazz.isEnum();
    }
}
