package com.enterprise.business.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base entity for history tracking - IMMUTABLE
 * Stores snapshots of entity state changes
 *
 * Note: This entity is immutable. Once created, it cannot be modified.
 * - Uses @Immutable for Hibernate optimization (no dirty checking)
 * - All columns marked as updatable = false
 * - No setters, only protected constructor for subclass builders
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Immutable
public abstract class HistoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "entity_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID entityId;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 100)
    private String entityType;

    @Column(name = "action", nullable = false, updatable = false, length = 20)
    private String action; // CREATE, UPDATE, DELETE, RESTORE

    @Column(name = "snapshot", columnDefinition = "jsonb", updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String snapshot; // JSON snapshot of entity state

    @Column(name = "diff", columnDefinition = "jsonb", updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String diff;

    @Column(name = "changed_by", nullable = false, updatable = false, length = 100)
    private String changedBy;

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @Column(name = "previous_status", length = 50, updatable = false)
    private String previousStatus;

    @Column(name = "current_status", length = 50, updatable = false)
    private String currentStatus;

    @Column(name = "ip_address", length = 50, updatable = false)
    private String ipAddress;

    /**
     * Protected no-arg constructor for JPA
     */
    protected HistoryEntity() {
    }

    /**
     * Protected all-args constructor for subclass builders
     * Ensures all fields are set at construction time
     */
    protected HistoryEntity(UUID entityId, String entityType, String action,
            String snapshot, String diff, String changedBy,
            String correlationId, String previousStatus, String currentStatus,
            String ipAddress) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.action = action;
        this.snapshot = snapshot;
        this.diff = diff;
        this.changedBy = changedBy;
        this.correlationId = correlationId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.ipAddress = ipAddress;
    }

    public Instant getChangedAt() {
        return getCreatedAt();
    }
}
