package com.enterprise.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.Instant;

/**
 * Soft Deletable Entity
 *
 * Features:
 * - Soft delete instead of hard delete
 * - Automatic filtering via @SQLRestriction (non-deleted records only)
 * - Track who deleted and when
 * - Can be restored
 *
 * Important:
 * - All JPA queries automatically exclude soft-deleted records
 * - To query deleted records, use native queries or enable the "includeDeleted"
 * filter
 *
 * Usage:
 * 
 * <pre>
 * @Entity
 * public class Organization extends SoftDeletableEntity<Long> {
 *     // Your fields here
 * }
 *
 * // Soft delete:
 * organization.softDelete("admin");
 * repository.save(organization);
 *
 * // Restore:
 * organization.restore();
 * repository.save(organization);
 *
 * // All standard JPA queries automatically filter deleted records:
 * repository.findAll(); // Only returns non-deleted
 * repository.findById(id); // Returns empty if soft-deleted
 * </pre>
 */
@MappedSuperclass
@Getter
@Setter
@SQLRestriction("deleted = false")
@FilterDef(name = "includeDeleted", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
public abstract class SoftDeletableEntity<ID extends Serializable> extends AuditableEntity<ID> {
    private static final long serialVersionUID = 1L;

    /**
     * Flag indicating if entity is deleted
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /**
     * User who deleted this entity
     */
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /**
     * Timestamp when entity was deleted
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Soft delete this entity
     *
     * @param deletedBy User performing the delete
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedBy = deletedBy;
        this.deletedAt = Instant.now();
    }

    /**
     * Restore a soft-deleted entity
     */
    public void restore() {
        this.deleted = false;
        this.deletedBy = null;
        this.deletedAt = null;
    }

    /**
     * Check if entity is deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Check if entity is active (not deleted)
     */
    public boolean isActive() {
        return !deleted;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, deleted=%s, deletedBy=%s, deletedAt=%s]",
                getClass().getSimpleName(), getId(), deleted, deletedBy, deletedAt);
    }
}
