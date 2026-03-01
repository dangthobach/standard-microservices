package com.enterprise.process.integration;

import com.enterprise.common.constant.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/connectors")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class IntegrationController {

    private final ConnectorService service;

    @GetMapping
    public List<ConnectorDefinition> getAll() {
        return service.getAllConnectors();
    }

    @GetMapping("/{id}")
    public ConnectorDefinition getOne(@PathVariable UUID id) {
        return service.getConnector(id);
    }

    @PostMapping
    public ConnectorDefinition createOrUpdate(@RequestBody ConnectorDefinition connector) {
        return service.saveConnector(connector);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteConnector(id);
        return ResponseEntity.noContent().build();
    }
}
