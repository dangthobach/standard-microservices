package com.enterprise.integration.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Connector Execution Log
 * <p>
 * Audit trail for all connector executions.
 * Stores request/response details, status, and duration.
 */
@Entity
@Table(name = "connector_executions", indexes = {
        @Index(name = "idx_exec_connector_id", columnList = "connector_config_id"),
        @Index(name = "idx_exec_status", columnList = "status"),
        @Index(name = "idx_exec_created", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorExecution extends BaseEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_config_id", nullable = false)
    private ConnectorConfig connectorConfig;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    private Long durationMs;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILURE

    private Integer statusCode; // HTTP status or error code

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
