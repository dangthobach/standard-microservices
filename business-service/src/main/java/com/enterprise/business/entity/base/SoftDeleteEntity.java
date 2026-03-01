package com.enterprise.business.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Entity with soft delete support
 * Extends AuditEntity to include audit trail
 *
 * IMPORTANT: @SQLRestriction automatically filters deleted records
 * - All JPQL queries (findAll, findById, etc.) will only return deleted = false
 * - To access deleted records, use native queries with "includeDeleted" suffix
 * - See SoftDeleteRepository for admin/audit methods
 */
@Getter
@Setter
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeleteEntity extends AuditEntity {

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean not null default false")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /**
     * Soft delete this entity
     * 
     * @param deletedBy Username of the person deleting
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Restore a soft deleted entity
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /**
     * Check if entity is deleted
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}
