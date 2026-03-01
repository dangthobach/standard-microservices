package com.enterprise.process.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConnectorRepository extends JpaRepository<ConnectorDefinition, UUID> {
}
