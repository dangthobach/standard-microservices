package com.enterprise.process.integration;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "connector_definition", schema = "flowable")
@Data
public class ConnectorDefinition {

    @Id
    private String id;

    private String name;
    private String type;
    private String description;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
