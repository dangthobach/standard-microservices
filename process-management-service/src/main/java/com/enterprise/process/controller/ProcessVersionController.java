package com.enterprise.process.controller;

import com.enterprise.process.service.DeploymentService;
import com.enterprise.process.service.ProcessVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Process Version Management Controller
 * Handles CRUD operations for process versions, comparison, and rollback
 */
@Slf4j
@RestController
@RequestMapping("/api/process-versions")
@RequiredArgsConstructor
public class ProcessVersionController {

    private final DeploymentService deploymentService;
    private final ProcessVersionService versionService;

    /**
     * Get all versions of a process definition by key
     */
    @GetMapping("/{processKey}/versions")
    public ResponseEntity<List<ProcessVersionDTO>> getProcessVersions(@PathVariable String processKey) {
        log.info("Fetching all versions for process: {}", processKey);
        
        List<ProcessDefinition> definitions = deploymentService.getProcessDefinitionHistory(processKey);
        
        List<ProcessVersionDTO> versions = definitions.stream()
                .map(pd -> new ProcessVersionDTO(
                        pd.getId(),
                        pd.getKey(),
                        pd.getName(),
                        pd.getVersion(),
                        pd.getDeploymentId(),
                        pd.getCategory(),
                        versionService.isActiveVersion(pd.getKey(), pd.getVersion())
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(versions);
    }

    /**
     * Compare two process versions
     */
    @GetMapping("/compare")
    public ResponseEntity<VersionComparisonDTO> compareVersions(
            @RequestParam String v1,
            @RequestParam String v2) {
        log.info("Comparing process versions: {} vs {}", v1, v2);
        
        VersionComparisonDTO comparison = versionService.compareVersions(v1, v2);
        return ResponseEntity.ok(comparison);
    }

    /**
     * Get metadata for a process version
     */
    @GetMapping("/{processDefinitionId}/metadata")
    public ResponseEntity<Map<String, Object>> getProcessMetadata(@PathVariable String processDefinitionId) {
        log.info("Fetching metadata for process: {}", processDefinitionId);
        
        Map<String, Object> metadata = versionService.getProcessMetadata(processDefinitionId);
        return ResponseEntity.ok(metadata);
    }

    /**
     * Rollback to a specific version (creates new deployment with old BPMN)
     */
    @PostMapping("/{processKey}/rollback")
    public ResponseEntity<ProcessVersionDTO> rollbackToVersion(
            @PathVariable String processKey,
            @RequestBody RollbackRequest request) {
        log.info("Rolling back process {} to version {}", processKey, request.getTargetVersion());
        
        ProcessDefinition newVersion = versionService.rollbackToVersion(processKey, request.getTargetVersion());
        
        ProcessVersionDTO dto = new ProcessVersionDTO(
                newVersion.getId(),
                newVersion.getKey(),
                newVersion.getName(),
                newVersion.getVersion(),
                newVersion.getDeploymentId(),
                newVersion.getCategory(),
                true
        );
        
        return ResponseEntity.ok(dto);
    }

    /**
     * Get active version for a process key
     */
    @GetMapping("/{processKey}/active")
    public ResponseEntity<ProcessVersionDTO> getActiveVersion(@PathVariable String processKey) {
        log.info("Fetching active version for process: {}", processKey);
        
        List<ProcessDefinition> definitions = deploymentService.getProcessDefinitionHistory(processKey);
        if (definitions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ProcessDefinition latest = definitions.get(0); // Latest version
        ProcessVersionDTO dto = new ProcessVersionDTO(
                latest.getId(),
                latest.getKey(),
                latest.getName(),
                latest.getVersion(),
                latest.getDeploymentId(),
                latest.getCategory(),
                true
        );
        
        return ResponseEntity.ok(dto);
    }

    // DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProcessVersionDTO {
        private String id;
        private String key;
        private String name;
        private int version;
        private String deploymentId;
        private String category;
        private boolean active;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class VersionComparisonDTO {
        private String version1Id;
        private String version2Id;
        private int version1Number;
        private int version2Number;
        private boolean identical;
        private String xmlDiff;
        private List<String> changes;
    }

    @lombok.Data
    public static class RollbackRequest {
        private int targetVersion;
        private String reason;
    }
}
