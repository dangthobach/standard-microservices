package com.enterprise.business.entity;

import com.enterprise.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Product Attachment Entity
 *
 * Extends AuditableEntity which provides:
 * - id (UUID), version (optimistic locking)
 * - createdBy, createdAt (auto-populated from JWT on INSERT)
 * - updatedBy, updatedAt (auto-populated on UPDATE)
 *
 * Tracks file attachments associated with products.
 */
@Entity
@Table(name = "product_attachments", indexes = {
        @Index(name = "idx_attachment_product", columnList = "product_id"),
        @Index(name = "idx_attachment_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttachment extends AuditableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;
}
