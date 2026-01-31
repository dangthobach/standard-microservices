package com.enterprise.process.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectorRepository extends JpaRepository<ConnectorDefinition, String> {
}
