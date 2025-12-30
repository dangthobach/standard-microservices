package com.enterprise.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Base Entity with ID and Version Control
 *
 * Features:
 * - Generic ID type support
 * - Optimistic locking with @Version
 * - Implements Persistable for better JPA integration
 * - Proper equals/hashCode based on ID
 *
 * Usage:
 * <pre>
 * @Entity
 * public class Organization extends BaseEntity<Long> {
 *     // Your fields here
 * }
 * </pre>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity<ID extends Serializable> implements Persistable<ID>, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private ID id;

    /**
     * Version field for optimistic locking
     * Automatically incremented by JPA on each update
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Check if entity is new (not persisted yet)
     * Used by Spring Data JPA to determine if save() should INSERT or UPDATE
     */
    @Override
    @Transient
    public boolean isNew() {
        return id == null;
    }

    /**
     * Equals based on ID
     * If both have no ID, use reference equality
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseEntity<?> that = (BaseEntity<?>) o;

        // If both are new (no ID), use reference equality
        if (this.isNew() && that.isNew()) {
            return false;
        }

        // If one is new and other is not, they are different
        if (this.isNew() || that.isNew()) {
            return false;
        }

        // Both have IDs, compare them
        return Objects.equals(id, that.id);
    }

    /**
     * HashCode based on ID
     * If no ID yet, use class hashCode
     */
    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, version=%s]",
            getClass().getSimpleName(), id, version);
    }
}
