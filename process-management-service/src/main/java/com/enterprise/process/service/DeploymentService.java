package com.enterprise.process.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.flowable.bpmn.model.BpmnModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    private final RepositoryService repositoryService;

    public List<Deployment> getAllDeployments() {
        return repositoryService.createDeploymentQuery()
                .orderByDeploymentTime().desc()
                .list();
    }

    public Deployment deployProcess(String name, String category, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.endsWith(".zip")) {
                return repositoryService.createDeployment()
                        .name(name)
                        .category(category)
                        .addZipInputStream(new ZipInputStream(inputStream))
                        .deploy();
            } else {
                return repositoryService.createDeployment()
                        .name(name)
                        .category(category)
                        .addInputStream(filename, inputStream)
                        .deploy();
            }
        } catch (IOException e) {
            log.error("Failed to deploy process", e);
            throw new RuntimeException("Failed to deploy process", e);
        }
    }

    public void deleteDeployment(String deploymentId) {
        repositoryService.deleteDeployment(deploymentId, true);
    }

    public BpmnModel getProcessDefinitionModel(String processDefinitionId) {
        return repositoryService.getBpmnModel(processDefinitionId);
    }

    public InputStream getProcessDefinitionResource(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        return repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
                processDefinition.getResourceName());
    }

    public List<ProcessDefinition> getProcessDefinitionHistory(String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion().desc()
                .list();
    }

    /**
     * Get all latest process definitions (one per key)
     */
    public List<ProcessDefinition> getAllLatestProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionKey().asc()
                .list();
    }

    /**
     * Deploy BPMN process from classpath resources
     * Used for automatic deployment on startup
     */
    public Deployment deployFromClasspath(String resourcePath, String deploymentName, String category) {
        log.info("Deploying process from classpath: {}", resourcePath);
        try {
            return repositoryService.createDeployment()
                    .name(deploymentName)
                    .category(category)
                    .addClasspathResource(resourcePath)
                    .deploy();
        } catch (Exception e) {
            log.error("Failed to deploy process from classpath: {}", resourcePath, e);
            throw new RuntimeException("Failed to deploy process from classpath", e);
        }
    }

    /**
     * Check if a process definition exists by key
     */
    public boolean processDefinitionExists(String processDefinitionKey) {
        long count = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .count();
        return count > 0;
    }
}
