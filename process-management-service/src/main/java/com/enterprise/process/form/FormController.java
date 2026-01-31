package com.enterprise.process.form;

import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forms")
@CrossOrigin(origins = "http://localhost:3000")
public class FormController {

    @Autowired
    private FormRepositoryService formRepositoryService;

    @Autowired
    private FormService formService;

    @GetMapping
    public List<Map<String, Object>> getForms() {
        return formRepositoryService.createFormDefinitionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(fd -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", fd.getId());
                    map.put("key", fd.getKey());
                    map.put("name", fd.getName());
                    map.put("version", fd.getVersion());
                    map.put("resourceName", fd.getResourceName());
                    map.put("deploymentId", fd.getDeploymentId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> deployForm(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "formKey", required = false) String formKey,
            @RequestParam(value = "formName", required = false) String formName) {

        try {
            FormDeployment deployment = formRepositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .name(formName != null ? formName : file.getOriginalFilename())
                    .deploy();

            return ResponseEntity.ok(Map.of(
                    "id", deployment.getId(),
                    "name", deployment.getName(),
                    "deploymentTime", deployment.getDeploymentTime()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{deploymentId}")
    public void deleteForm(@PathVariable String deploymentId) {
        formRepositoryService.deleteDeployment(deploymentId, true);
    }

    @GetMapping("/{id}/model")
    public ResponseEntity<String> getFormModel(@PathVariable String id) {
        try (InputStream is = formRepositoryService.getFormDefinitionResource(id)) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
