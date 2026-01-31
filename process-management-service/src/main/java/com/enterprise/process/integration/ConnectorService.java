package com.enterprise.process.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConnectorService {

    @Autowired
    private ConnectorRepository repository;

    public List<ConnectorDefinition> getAllConnectors() {
        return repository.findAll();
    }

    public ConnectorDefinition getConnector(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Connector not found"));
    }

    public ConnectorDefinition saveConnector(ConnectorDefinition connector) {
        return repository.save(connector);
    }

    public void deleteConnector(String id) {
        repository.deleteById(id);
    }
}
