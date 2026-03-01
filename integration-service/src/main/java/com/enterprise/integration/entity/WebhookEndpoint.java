package com.enterprise.integration.entity;

import com.enterprise.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Webhook Endpoint Configuration
 * <p>
 * Defines an endpoint for receiving external webhooks.
 * Includes security settings (HMAC secret) and event filtering.
 */
@Entity
@Table(name = "webhook_endpoints", indexes = {
        @Index(name = "idx_webhook_path", columnList = "path"),
        @Index(name = "idx_webhook_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEndpoint extends AuditableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String path; // e.g., /webhooks/stripe

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String secret; // HMAC secret for signature verification

    @Column(name = "header_name", length = 100)
    private String headerName; // Header containing the signature (e.g., X-Hub-Signature)

    @Column(nullable = false)
    private Boolean active = true;

    // JSON array of event types to process (e.g., ["payment.success",
    // "payment.failed"])
    @Column(name = "events", columnDefinition = "TEXT")
    private String events;
}
