package com.enterprise.business.entity;

import com.enterprise.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku"),
        @Index(name = "idx_product_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends SoftDeletableEntity<UUID> {

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

    // Workflow Fields
    @Column(length = 50)
    private String status = "DRAFT"; // DRAFT, PENDING_APPROVAL, PENDING_CONFIRMATION, ACTIVE, REJECTED

    @Column(length = 64)
    private String processInstanceId;

    @Builder
    public Product(UUID id, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt,
            boolean deleted, String deletedBy, Instant deletedAt,
            String name, String sku, String description, BigDecimal price, String category,
            Integer stockQuantity, Boolean active,
            String status, String processInstanceId) {
        this.setId(id);
        this.setCreatedBy(createdBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedBy(updatedBy);
        this.setUpdatedAt(updatedAt);
        this.setDeleted(deleted);
        this.setDeletedBy(deletedBy);
        this.setDeletedAt(deletedAt);
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.active = active;
        this.status = status != null ? status : "DRAFT";
        this.processInstanceId = processInstanceId;
    }
}
