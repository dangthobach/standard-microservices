package com.enterprise.business.entity;

import com.enterprise.business.entity.base.StatefulEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product Entity
 *
 * Extends StatefulEntity which provides:
 * - id (UUID), version (optimistic locking)
 * - createdBy, createdAt, updatedBy, updatedAt (audit)
 * - deleted, deletedBy, deletedAt (soft delete, auto-filtered)
 * - status, previousStatus, statusChangedAt/By/Reason (state machine)
 *
 * State Machine (defined in canTransitionTo):
 * - DRAFT -> PENDING_APPROVAL
 * - PENDING_APPROVAL -> APPROVED | REJECTED
 * - APPROVED -> ACTIVE
 * - REJECTED -> DRAFT
 * - ACTIVE -> INACTIVE
 * - INACTIVE -> ACTIVE
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku"),
        @Index(name = "idx_product_category", columnList = "category"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends StatefulEntity<ProductStatus, ProductHistory> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Reference to the workflow process instance (if any)
     */
    @Column(length = 64)
    private String processInstanceId;

    /**
     * Initialize status to DRAFT if not set
     */
    @PostLoad
    @PrePersist
    protected void initializeStatus() {
        if (getStatus() == null) {
            setStatus(ProductStatus.DRAFT);
        }
    }

    /**
     * State transition validation
     */
    @Override
    public boolean canTransitionTo(ProductStatus newStatus) {
        if (getStatus() == null) {
            return newStatus == ProductStatus.DRAFT;
        }

        return switch (getStatus()) {
            case DRAFT -> newStatus == ProductStatus.PENDING_APPROVAL;
            case PENDING_APPROVAL -> newStatus == ProductStatus.APPROVED
                    || newStatus == ProductStatus.REJECTED;
            case APPROVED -> newStatus == ProductStatus.ACTIVE;
            case REJECTED -> newStatus == ProductStatus.DRAFT;
            case ACTIVE -> newStatus == ProductStatus.INACTIVE;
            case INACTIVE -> newStatus == ProductStatus.ACTIVE;
        };
    }

    @Override
    public ProductHistory createHistorySnapshot(
            String action,
            ProductStatus previousStatus,
            String changedBy,
            String snapshot,
            String diff,
            String correlationId) {

        return ProductHistory.builder()
                .entityId(this.getId())
                .action(action)
                .snapshot(snapshot)
                .diff(diff)
                .changedBy(changedBy)
                .correlationId(correlationId)
                .previousStatus(previousStatus != null ? previousStatus.name() : null)
                .currentStatus(this.getStatus() != null ? this.getStatus().name() : null)
                // Note: ipAddress might not be available at entity level, can be set in service
                .build();
    }

    @Builder
    public Product(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
            boolean deleted, String deletedBy, Instant deletedAt,
            String name, String sku, String description, BigDecimal price, String category,
            Integer stockQuantity, Boolean active,
            ProductStatus status, String processInstanceId) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.setStatus(status != null ? status : ProductStatus.DRAFT);
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.active = active != null ? active : true;
        this.processInstanceId = processInstanceId;
    }
}
