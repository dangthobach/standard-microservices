package com.enterprise.integration.repository;

import com.enterprise.integration.entity.ConnectorExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ConnectorExecutionRepository extends JpaRepository<ConnectorExecution, UUID> {

    Page<ConnectorExecution> findByConnectorConfigId(UUID connectorConfigId, Pageable pageable);

    Page<ConnectorExecution> findByStatus(String status, Pageable pageable);

    Page<ConnectorExecution> findByStartedAtBetween(Instant start, Instant end, Pageable pageable);
}
