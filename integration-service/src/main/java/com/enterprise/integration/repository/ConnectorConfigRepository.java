package com.enterprise.integration.repository;

import com.enterprise.integration.connector.ConnectorType;
import com.enterprise.integration.entity.ConnectorConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectorConfigRepository extends JpaRepository<ConnectorConfig, UUID> {

    Optional<ConnectorConfig> findByName(String name);

    Page<ConnectorConfig> findByType(ConnectorType type, Pageable pageable);

    boolean existsByName(String name);
}
