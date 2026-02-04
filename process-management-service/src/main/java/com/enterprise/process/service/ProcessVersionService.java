package com.enterprise.process.service;

import com.enterprise.process.controller.ProcessVersionController.VersionComparisonDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for managing process versions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessVersionService {

    private final RepositoryService repositoryService;

    /**
     * Check if a version is the active (latest) version
     */
    public boolean isActiveVersion(String processKey, int version) {
        ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
        
        return latest != null && latest.getVersion() == version;
    }

    /**
     * Compare two process versions
     */
    public VersionComparisonDTO compareVersions(String version1Id, String version2Id) {
        try {
            ProcessDefinition pd1 = repositoryService.getProcessDefinition(version1Id);
            ProcessDefinition pd2 = repositoryService.getProcessDefinition(version2Id);

            String xml1 = getProcessXml(version1Id);
            String xml2 = getProcessXml(version2Id);

            boolean identical = xml1.equals(xml2);
            List<String> changes = new ArrayList<>();
            
            if (!identical) {
                changes.add("BPMN XML content differs between versions");
                // Could add more sophisticated diff analysis here
            }

            return new VersionComparisonDTO(
                    version1Id,
                    version2Id,
                    pd1.getVersion(),
                    pd2.getVersion(),
                    identical,
                    identical ? "No changes" : "Versions differ - detailed diff not implemented",
                    changes
            );
        } catch (Exception e) {
            log.error("Failed to compare versions", e);
            throw new RuntimeException("Failed to compare versions", e);
        }
    }

    /**
     * Get metadata for a process definition
     */
    public Map<String, Object> getProcessMetadata(String processDefinitionId) {
        ProcessDefinition pd = repositoryService.getProcessDefinition(processDefinitionId);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", pd.getId());
        metadata.put("key", pd.getKey());
        metadata.put("name", pd.getName());
        metadata.put("version", pd.getVersion());
        metadata.put("deploymentId", pd.getDeploymentId());
        metadata.put("category", pd.getCategory());
        metadata.put("resourceName", pd.getResourceName());
        metadata.put("diagramResourceName", pd.getDiagramResourceName());
        metadata.put("hasStartFormKey", pd.hasStartFormKey());
        metadata.put("suspended", pd.isSuspended());
        metadata.put("tenantId", pd.getTenantId());
        
        // Get deployment info
        Deployment deployment = repositoryService.createDeploymentQuery()
                .deploymentId(pd.getDeploymentId())
                .singleResult();
        
        if (deployment != null) {
            metadata.put("deploymentName", deployment.getName());
            metadata.put("deploymentTime", deployment.getDeploymentTime());
        }
        
        return metadata;
    }

    /**
     * Rollback to a previous version by redeploying that version's BPMN
     */
    public ProcessDefinition rollbackToVersion(String processKey, int targetVersion) {
        log.info("Rolling back process {} to version {}", processKey, targetVersion);
        
        // Find the target version
        ProcessDefinition targetPd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .processDefinitionVersion(targetVersion)
                .singleResult();
        
        if (targetPd == null) {
            throw new IllegalArgumentException("Target version not found: " + targetVersion);
        }
        
        // Get the BPMN XML from target version
        String xml = getProcessXml(targetPd.getId());
        
        // Deploy as a new version
        Deployment deployment = repositoryService.createDeployment()
                .name("Rollback to version " + targetVersion)
                .category(targetPd.getCategory())
                .addString(targetPd.getResourceName(), xml)
                .deploy();
        
        log.info("Rollback deployment created: {}", deployment.getId());
        
        // Get the newly created process definition
        ProcessDefinition newVersion = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        
        return newVersion;
    }

    /**
     * Get BPMN XML for a process definition
     */
    private String getProcessXml(String processDefinitionId) {
        try {
            ProcessDefinition pd = repositoryService.getProcessDefinition(processDefinitionId);
            InputStream is = repositoryService.getResourceAsStream(
                    pd.getDeploymentId(),
                    pd.getResourceName()
            );
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to get process XML", e);
            throw new RuntimeException("Failed to get process XML", e);
        }
    }
}
