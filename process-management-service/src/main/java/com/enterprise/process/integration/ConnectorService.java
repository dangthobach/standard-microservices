package com.enterprise.process.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectorService {

    private final ConnectorRepository repository;

    public List<ConnectorDefinition> getAllConnectors() {
        return repository.findAll();
    }

    public ConnectorDefinition getConnector(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connector not found: " + id));
    }

    public ConnectorDefinition saveConnector(ConnectorDefinition connector) {
        return repository.save(connector);
    }

    public void deleteConnector(UUID id) {
        ConnectorDefinition connector = getConnector(id);
        connector.softDelete("system"); // Soft delete instead of hard delete
        repository.save(connector);
    }
}
