package com.enterprise.process.config;

import com.enterprise.process.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.repository.Deployment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Automatically deploy BPMN processes from classpath on application startup.
 * This ensures critical processes are always available.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessDeploymentRunner implements CommandLineRunner {

    private final DeploymentService deploymentService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting automatic process deployment...");

        // Deploy Product Approval Process
        deployProductApprovalProcess();

        log.info("Automatic process deployment completed");
    }

    private void deployProductApprovalProcess() {
        String processKey = "product-approval-process";
        String resourcePath = "processes/product-creation.bpmn20.xml";

        if (!deploymentService.processDefinitionExists(processKey)) {
            log.info("Deploying Product Approval Process for the first time...");
            try {
                Deployment deployment = deploymentService.deployFromClasspath(
                        resourcePath,
                        "Product Approval Process - Auto Deployment",
                        "product-management"
                );
                log.info("Successfully deployed Product Approval Process: deploymentId={}", deployment.getId());
            } catch (Exception e) {
                log.error("Failed to deploy Product Approval Process", e);
                // Don't throw - allow application to start even if deployment fails
            }
        } else {
            log.info("Product Approval Process already exists, skipping deployment");
        }
    }
}
