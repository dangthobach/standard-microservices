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
}

