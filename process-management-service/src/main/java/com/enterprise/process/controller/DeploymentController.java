package com.enterprise.process.controller;

import com.enterprise.process.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.repository.Deployment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<DeploymentDTO>> getAllDeployments() {
        List<Deployment> deployments = deploymentService.getAllDeployments();
        List<DeploymentDTO> dtos = deployments.stream()
                .map(d -> new DeploymentDTO(d.getId(), d.getName(), d.getCategory(), d.getDeploymentTime()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<DeploymentDTO> deploy(@RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("file") MultipartFile file) {
        Deployment deployment = deploymentService.deployProcess(name, category, file);
        return ResponseEntity.ok(new DeploymentDTO(deployment.getId(), deployment.getName(), deployment.getCategory(),
                deployment.getDeploymentTime()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeployment(@PathVariable String id) {
        deploymentService.deleteDeployment(id);
        return ResponseEntity.noContent().build();
    }

    // Simple DTO for response
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DeploymentDTO {
        private String id;
        private String name;
        private String category;
        private java.util.Date deploymentTime;
    }
}
