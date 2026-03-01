package com.enterprise.integration.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Webhook Event Log
 * <p>
 * log of received webhook events.
 */
@Entity
@Table(name = "webhook_events", indexes = {
        @Index(name = "idx_event_endpoint_id", columnList = "webhook_endpoint_id"),
        @Index(name = "idx_event_status", columnList = "status"),
        @Index(name = "idx_event_received", columnList = "received_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent extends BaseEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_endpoint_id", nullable = false)
    private WebhookEndpoint webhookEndpoint;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(nullable = false, length = 20)
    private String status; // PROCESSED, FAILED, IGNORED

    @Column(columnDefinition = "TEXT")
    private String processingExcepion;

    @Column(nullable = false)
    private Long processingTimeMs;
}
