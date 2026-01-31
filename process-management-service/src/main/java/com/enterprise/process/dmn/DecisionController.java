package com.enterprise.process.dmn;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dmn")
@CrossOrigin(origins = "http://localhost:3000")
public class DecisionController {

    @Autowired
    private DmnDecisionService decisionService;

    @Autowired
    private DmnRepositoryService dmnRepositoryService;

    @Autowired
    private DmnHistoryService dmnHistoryService;

    // Decision Definitions
    @GetMapping("/definitions")
    public List<Map<String, Object>> getDecisionDefinitions() {
        return dmnRepositoryService.createDecisionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(dd -> {
                    Map<String, Object> def = new HashMap<>();
                    def.put("id", dd.getId());
                    def.put("key", dd.getKey());
                    def.put("name", dd.getName());
                    def.put("version", dd.getVersion());
                    def.put("category", dd.getCategory());
                    def.put("deploymentId", dd.getDeploymentId());
                    def.put("resourceName", dd.getResourceName());
                    return def;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/definitions/{key}")
    public Map<String, Object> getDecisionDefinition(@PathVariable("key") String key) {
        var dd = dmnRepositoryService.createDecisionQuery()
                .decisionKey(key)
                .latestVersion()
                .singleResult();

        if (dd == null) {
            throw new RuntimeException("Decision definition not found");
        }

        Map<String, Object> def = new HashMap<>();
        def.put("id", dd.getId());
        def.put("key", dd.getKey());
        def.put("name", dd.getName());
        def.put("version", dd.getVersion());
        def.put("category", dd.getCategory());
        def.put("deploymentId", dd.getDeploymentId());
        def.put("resourceName", dd.getResourceName());
        return def;
    }

    // Execute Decisions
    @PostMapping("/decisions/{key}/execute")
    public Map<String, Object> executeDecision(@PathVariable("key") String decisionKey,
            @RequestBody Map<String, Object> variables) {
        var result = decisionService.createExecuteDecisionBuilder()
                .decisionKey(decisionKey)
                .variables(variables)
                .executeWithSingleResult();
        return Map.of("decisionKey", decisionKey, "result", result);
    }

    @PostMapping("/decisions/{key}/execute-all")
    public Map<String, Object> executeDecisionAllResults(@PathVariable("key") String decisionKey,
            @RequestBody Map<String, Object> variables) {
        var results = decisionService.createExecuteDecisionBuilder()
                .decisionKey(decisionKey)
                .variables(variables)
                .execute();

        List<Map<String, Object>> resultList = results.stream()
                .map(result -> {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("resultVariables", result);
                    resultMap.put("ruleId", "unknown");
                    resultMap.put("ruleName", "unknown");
                    return resultMap;
                })
                .collect(Collectors.toList());

        return Map.of("decisionKey", decisionKey, "results", resultList);
    }

    // Decision Tables
    @GetMapping("/tables")
    public List<Map<String, Object>> getDecisionTables() {
        return dmnRepositoryService.createDecisionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(dd -> {
                    Map<String, Object> table = new HashMap<>();
                    table.put("id", dd.getId());
                    table.put("key", dd.getKey());
                    table.put("name", dd.getName());
                    table.put("version", dd.getVersion());
                    table.put("category", dd.getCategory());
                    return table;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/tables/{key}")
    public Map<String, Object> getDecisionTable(@PathVariable("key") String key) {
        var dd = dmnRepositoryService.createDecisionQuery()
                .decisionKey(key)
                .latestVersion()
                .singleResult();

        if (dd == null) {
            throw new RuntimeException("Decision table not found");
        }

        Map<String, Object> table = new HashMap<>();
        table.put("id", dd.getId());
        table.put("key", dd.getKey());
        table.put("name", dd.getName());
        table.put("version", dd.getVersion());
        table.put("category", dd.getCategory());
        table.put("deploymentId", dd.getDeploymentId());
        table.put("resourceName", dd.getResourceName());
        return table;
    }

    // History
    @GetMapping("/history")
    public List<Map<String, Object>> getDecisionHistory() {
        return dmnHistoryService.createHistoricDecisionExecutionQuery()
                .list()
                .stream()
                .map(hde -> {
                    Map<String, Object> execution = new HashMap<>();
                    execution.put("id", hde.getId());
                    execution.put("decisionKey", hde.getDecisionKey());
                    execution.put("decisionName", hde.getDecisionName());
                    execution.put("instanceId", hde.getInstanceId());
                    execution.put("executionTime", hde.getStartTime());
                    execution.put("endTime", hde.getEndTime());
                    return execution;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/history/{id}")
    public Map<String, Object> getDecisionExecution(@PathVariable("id") String id) {
        var hde = dmnHistoryService.createHistoricDecisionExecutionQuery()
                .executionId(id)
                .singleResult();

        if (hde == null) {
            throw new RuntimeException("Decision execution not found");
        }

        Map<String, Object> execution = new HashMap<>();
        execution.put("id", hde.getId());
        execution.put("decisionKey", hde.getDecisionKey());
        execution.put("decisionName", hde.getDecisionName());
        execution.put("instanceId", hde.getInstanceId());
        execution.put("executionTime", hde.getStartTime());
        execution.put("endTime", hde.getEndTime());
        execution.put("inputVariables", new HashMap<>());
        execution.put("outputVariables", new HashMap<>());
        return execution;
    }

    // Deploy decision
    @PostMapping("/deploy")
    public Map<String, Object> deployDecision(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "decisionKey", required = false) String decisionKey,
            @RequestParam(value = "decisionName", required = false) String decisionName) {
        try {
            var deployment = dmnRepositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .name(decisionName != null ? decisionName : file.getOriginalFilename())
                    .deploy();

            return Map.of("id", deployment.getId(), "name", deployment.getName(), "deploymentTime",
                    deployment.getDeploymentTime());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to deploy decision", e);
        }
    }

    // Delete decision (by deployment ID to be safe, or decision definition ID if
    // cascading)
    // The frontend sends decisionId which usually maps to a deployment or
    // definition.
    // Flowable typically deletes deployments. Let's assume the ID passed is the
    // DEPLOYMENT ID for now,
    // or we might need to look up the deployment ID from the decision definition
    // ID.
    // Looking at the frontend, it deletes by 'decisionId'. If it's a decision
    // definition ID, we need to find its deployment.
    @DeleteMapping("/decisions/{id}")
    public void deleteDecision(@PathVariable("id") String id) {
        // Check if it looks like a definition ID or deployment ID.
        // For simplicity, let's try to delete as deployment first, if fail, try
        // definition.
        // Actually, dmnRepositoryService.deleteDeployment(id) is the standard way.
        // But we need to know if 'id' is deploymentId.
        // Let's assume the UI sends the DecisionDefinitionID, so we get the
        // deploymentId from it.

        var def = dmnRepositoryService.createDecisionQuery().decisionId(id).singleResult();
        if (def != null) {
            dmnRepositoryService.deleteDeployment(def.getDeploymentId());
        } else {
            // changes usually cascade, but let's try deleting deployment directly if it was
            // a deployment ID
            dmnRepositoryService.deleteDeployment(id);
        }
    }

    // Get DMN XML
    @GetMapping("/definitions/{id}/xml")
    public org.springframework.http.ResponseEntity<String> getDecisionDefinitionXml(@PathVariable("id") String id) {
        try (java.io.InputStream is = dmnRepositoryService.getDmnResource(id)) {
            String xml = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return org.springframework.http.ResponseEntity.ok(xml);
        } catch (java.io.IOException e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }
}
