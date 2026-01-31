package com.enterprise.process.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
@CrossOrigin(origins = "http://localhost:3000")
public class IntegrationController {

    @Autowired
    private ConnectorService service;

    @GetMapping
    public List<ConnectorDefinition> getAll() {
        return service.getAllConnectors();
    }

    @GetMapping("/{id}")
    public ConnectorDefinition getOne(@PathVariable String id) {
        return service.getConnector(id);
    }

    @PostMapping
    public ConnectorDefinition createOrUpdate(@RequestBody ConnectorDefinition connector) {
        return service.saveConnector(connector);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteConnector(id);
        return ResponseEntity.ok().build();
    }
}
