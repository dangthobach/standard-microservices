package com.enterprise.process.integration;

import com.enterprise.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Connector Definition Entity
 *
 * Extends SoftDeletableEntity which provides:
 * - id (UUID), version (optimistic locking)
 * - createdBy, createdAt, updatedBy, updatedAt (audit)
 * - deleted, deletedBy, deletedAt (soft delete, auto-filtered)
 *
 * Defines integration connectors for the process management service.
 */
@Entity
@Table(name = "connector_definition", indexes = {
        @Index(name = "idx_connector_name", columnList = "name"),
        @Index(name = "idx_connector_type", columnList = "type"),
        @Index(name = "idx_connector_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorDefinition extends SoftDeletableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Builder
    public ConnectorDefinition(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
            boolean deleted, String deletedBy, Instant deletedAt,
            String name, String type, String description, String status,
            String configuration) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.name = name;
        this.type = type;
        this.description = description;
        this.status = status;
        this.configuration = configuration;
    }
}
