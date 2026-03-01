package com.enterprise.business.repository.base;

import com.enterprise.business.entity.base.AuditEntity;
import com.enterprise.business.entity.base.SoftDeleteEntity;
import com.enterprise.business.entity.base.StatefulEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

/**
 * Reusable Specification building blocks
 * Tránh viết lại WHERE conditions ở mọi repository
 */
public class BaseSpecifications {

    public static <T extends AuditEntity> Specification<T> createdAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static <T extends AuditEntity> Specification<T> createdBefore(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static <T extends AuditEntity> Specification<T> createdBy(String user) {
        return (root, query, cb) -> cb.equal(root.get("createdBy"), user);
    }

    public static <T extends AuditEntity> Specification<T> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), from),
                cb.lessThanOrEqualTo(root.get("createdAt"), to));
    }

    public static <T extends SoftDeleteEntity> Specification<T> isActive() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static <T extends SoftDeleteEntity> Specification<T> isDeleted() {
        return (root, query, cb) -> cb.isTrue(root.get("deleted"));
    }

    public static <T extends StatefulEntity<?, ?>> Specification<T> hasStatus(Enum<?> status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static <T extends StatefulEntity<?, ?>> Specification<T> hasStatusIn(List<? extends Enum<?>> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static <T extends AuditEntity> Specification<T> updatedAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), from);
    }

    public static <T extends AuditEntity> Specification<T> updatedBy(String user) {
        return (root, query, cb) -> cb.equal(root.get("updatedBy"), user);
    }

    /**
     * WARNING: fieldName phải là static constant từ code, KHÔNG nhận từ user input.
     * value được parameterized an toàn qua CriteriaBuilder.
     * 
     * SAFE: fieldContains("name", userInput)
     * UNSAFE: fieldContains(userInput, "someValue")
     */
    public static <T> Specification<T> fieldContains(String fieldName, String value) {
        return (root, query, cb) -> {
            String escaped = value.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            return cb.like(cb.lower(root.get(fieldName)), "%" + escaped + "%");
        };
    }

    public static <T> Specification<T> fieldEquals(String fieldName, Object value) {
        return (root, query, cb) -> cb.equal(root.get(fieldName), value);
    }
}
