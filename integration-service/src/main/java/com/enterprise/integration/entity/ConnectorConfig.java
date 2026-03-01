package com.enterprise.integration.entity;

import com.enterprise.common.entity.AuditableEntity;
import com.enterprise.integration.connector.AuthorizationType;
import com.enterprise.integration.connector.ConnectorType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Connector Configuration Entity
 * <p>
 * Stores configuration for third-party integrations.
 * Includes connection details, authentication, and headers.
 */
@Entity
@Table(name = "connector_configs", indexes = {
        @Index(name = "idx_connector_name", columnList = "name"),
        @Index(name = "idx_connector_type", columnList = "type"),
        @Index(name = "idx_connector_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorConfig extends AuditableEntity<UUID> {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectorType type;

    @Column(nullable = false)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthorizationType authType;

    @Column(columnDefinition = "TEXT")
    private String authConfig; // JSON string for auth details (username, password, token, etc.)

    @Column(columnDefinition = "TEXT")
    private String defaultHeaders; // JSON string for default headers

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer timeoutSeconds = 30;

    // Retry policy configuration
    @Column(nullable = false)
    private Integer maxRetries = 3;

    @Column(nullable = false)
    private Long retryBackoffMs = 1000L;
}
