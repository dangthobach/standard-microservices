package com.enterprise.integration.controller;

import com.enterprise.common.constant.ApiConstants;
import com.enterprise.common.dto.ApiResponse;
import com.enterprise.integration.dto.ConnectorRequest;
import com.enterprise.integration.dto.ConnectorResponse;
import com.enterprise.integration.entity.ConnectorConfig;
import com.enterprise.integration.entity.ConnectorExecution;
import com.enterprise.integration.repository.ConnectorConfigRepository;
import com.enterprise.integration.repository.ConnectorExecutionRepository;
import com.enterprise.integration.service.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/connectors")
@RequiredArgsConstructor
@Tag(name = "Connector Management", description = "Manage and execute 3rd party connectors")
public class ConnectorController {

    private final ConnectorService connectorService;
    private final ConnectorConfigRepository configRepository;
    private final ConnectorExecutionRepository executionRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('connector:read')")
    @Operation(summary = "List connectors", description = "Get paginated list of connector configurations")
    public ResponseEntity<ApiResponse<Page<ConnectorConfig>>> listConnectors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ConnectorConfig> result = configRepository.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Connectors retrieved", result));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('connector:write')")
    @Operation(summary = "Create connector", description = "Register a new connector configuration")
    public ResponseEntity<ApiResponse<ConnectorConfig>> createConnector(@RequestBody ConnectorConfig config) {
        ConnectorConfig created = connectorService.createConnector(config);
        return ResponseEntity.ok(ApiResponse.success("Connector created", created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('connector:read')")
    public ResponseEntity<ApiResponse<ConnectorConfig>> getConnector(@PathVariable UUID id) {
        return configRepository.findById(id)
                .map(c -> ResponseEntity.ok(ApiResponse.success("Connector retrieved", c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{name}/execute")
    @PreAuthorize("hasAuthority('connector:execute')")
    @Operation(summary = "Execute connector", description = "Trigger a connector execution by name")
    public ResponseEntity<ApiResponse<ConnectorResponse>> executeConnector(
            @PathVariable String name,
            @RequestBody(required = false) ConnectorRequest request) {

        if (request == null) {
            request = new ConnectorRequest();
        }

        ConnectorResponse response = connectorService.executeConnector(name, request);
        return ResponseEntity.ok(ApiResponse.success("Execution completed", response));
    }

    @GetMapping("/executions")
    @PreAuthorize("hasAuthority('connector:read')")
    @Operation(summary = "List executions", description = "Get audit trail of connector executions")
    public ResponseEntity<ApiResponse<Page<ConnectorExecution>>> listExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ConnectorExecution> result = executionRepository.findAll(
                PageRequest.of(page, size, Sort.by("startedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Executions retrieved", result));
    }
}
