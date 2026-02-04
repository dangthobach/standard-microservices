package com.enterprise.process.controller;

import com.enterprise.process.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process-definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final DeploymentService deploymentService;

    @GetMapping("/{id}/model")
    public ResponseEntity<BpmnModel> getProcessModel(@PathVariable String id) {
        BpmnModel model = deploymentService.getProcessDefinitionModel(id);
        return ResponseEntity.ok(model);
    }

    @GetMapping
    public ResponseEntity<List<ProcessDefinitionDTO>> getProcessDefinitions(
            @RequestParam(required = false) String key) {
        List<ProcessDefinition> definitions;
        if (key != null) {
            definitions = deploymentService.getProcessDefinitionHistory(key);
        } else {
            // Get all latest versions
            definitions = deploymentService.getAllLatestProcessDefinitions();
        }

        List<ProcessDefinitionDTO> dtos = definitions.stream()
                .map(pd -> new ProcessDefinitionDTO(pd.getId(), pd.getKey(), pd.getName(), pd.getVersion(),
                        pd.getCategory()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<String> getProcessDefinitionXml(@PathVariable String id) {
        try (java.io.InputStream is = deploymentService.getProcessDefinitionResource(id)) {
            String xml = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(xml);
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProcessDefinitionDTO {
        private String id;
        private String key;
        private String name;
        private int version;
        private String category;
    }
}
