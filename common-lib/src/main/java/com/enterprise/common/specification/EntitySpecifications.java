package com.enterprise.common.specification;

import com.enterprise.common.entity.SoftDeletableEntity;
import com.enterprise.common.entity.StatefulEntity;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.time.Instant;

/**
 * Reusable JPA Specifications for common entity queries.
 *
 * Usage:
 * 
 * <pre>
 * // Combine specifications:
 * var spec = EntitySpecifications.<Product, UUID, ProductStatus>hasStatus(ProductStatus.ACTIVE)
 *         .and(EntitySpecifications.createdAfter(someDate));
 * List<Product> results = productRepository.findAll(spec);
 * </pre>
 */
public final class EntitySpecifications {

    private EntitySpecifications() {
        // Utility class - prevent instantiation
    }

    // ==================== Audit Specifications ====================

    /**
     * Filter entities created by a specific user
     */
    public static <T> Specification<T> createdBy(String username) {
        return (root, query, cb) -> cb.equal(root.get("createdBy"), username);
    }

    /**
     * Filter entities created after a specific timestamp
     */
    public static <T> Specification<T> createdAfter(Instant timestamp) {
        return (root, query, cb) -> cb.greaterThan(root.get("createdAt"), timestamp);
    }

    /**
     * Filter entities created before a specific timestamp
     */
    public static <T> Specification<T> createdBefore(Instant timestamp) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), timestamp);
    }

    /**
     * Filter entities created between two timestamps
     */
    public static <T> Specification<T> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }

    /**
     * Filter entities last modified by a specific user
     */
    public static <T> Specification<T> updatedBy(String username) {
        return (root, query, cb) -> cb.equal(root.get("updatedBy"), username);
    }

    /**
     * Filter entities updated after a specific timestamp
     */
    public static <T> Specification<T> updatedAfter(Instant timestamp) {
        return (root, query, cb) -> cb.greaterThan(root.get("updatedAt"), timestamp);
    }

    // ==================== Status Specifications ====================

    /**
     * Filter entities by current status (for StatefulEntity subclasses)
     */
    public static <T extends StatefulEntity<ID, S>, ID extends Serializable, S extends Enum<S>> Specification<T> hasStatus(
            S status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filter entities by any of the given statuses
     */
    @SafeVarargs
    public static <T extends StatefulEntity<ID, S>, ID extends Serializable, S extends Enum<S>> Specification<T> hasAnyStatus(
            S... statuses) {
        return (root, query, cb) -> root.get("status").in((Object[]) statuses);
    }

    /**
     * Filter entities NOT in specific status
     */
    public static <T extends StatefulEntity<ID, S>, ID extends Serializable, S extends Enum<S>> Specification<T> notInStatus(
            S status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }

    /**
     * Filter entities whose status was changed by a specific user
     */
    public static <T> Specification<T> statusChangedBy(String username) {
        return (root, query, cb) -> cb.equal(root.get("statusChangedBy"), username);
    }

    // ==================== Soft Delete Specifications ====================
    // Note: With @SQLRestriction these are rarely needed, but useful for native
    // queries

    /**
     * Explicitly filter non-deleted entities (useful in JPQL when @SQLRestriction
     * is bypassed)
     */
    public static <T extends SoftDeletableEntity<ID>, ID extends Serializable> Specification<T> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    /**
     * Filter entities deleted by a specific user
     * Note: This spec will not work with standard queries due to @SQLRestriction.
     * Use with native queries or when filter is temporarily disabled.
     */
    public static <T extends SoftDeletableEntity<ID>, ID extends Serializable> Specification<T> deletedBy(
            String username) {
        return (root, query, cb) -> cb.equal(root.get("deletedBy"), username);
    }
}
