package com.enterprise.process.cmmn;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cmmn")
@CrossOrigin(origins = "http://localhost:3000")
public class CaseController {

    @Autowired
    private CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    private CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    private CmmnTaskService cmmnTaskService;

    @Autowired
    private CmmnHistoryService cmmnHistoryService;

    // Case Definitions
    @GetMapping("/definitions")
    public List<Map<String, Object>> getCaseDefinitions() {
        return cmmnRepositoryService.createCaseDefinitionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(cd -> {
                    Map<String, Object> def = new HashMap<>();
                    def.put("id", cd.getId());
                    def.put("key", cd.getKey());
                    def.put("name", cd.getName());
                    def.put("version", cd.getVersion());
                    def.put("category", cd.getCategory());
                    def.put("deploymentId", cd.getDeploymentId());
                    def.put("resourceName", cd.getResourceName());
                    def.put("suspended", false); // CMMN doesn't have isSuspended method
                    return def;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/definitions/{key}")
    public Map<String, Object> getCaseDefinition(@PathVariable("key") String key) {
        CaseDefinition cd = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey(key)
                .latestVersion()
                .singleResult();
        
        if (cd == null) {
            throw new RuntimeException("Case definition not found");
        }

        Map<String, Object> def = new HashMap<>();
        def.put("id", cd.getId());
        def.put("key", cd.getKey());
        def.put("name", cd.getName());
        def.put("version", cd.getVersion());
        def.put("category", cd.getCategory());
        def.put("deploymentId", cd.getDeploymentId());
        def.put("resourceName", cd.getResourceName());
        def.put("suspended", false); // CMMN doesn't have isSuspended method
        return def;
    }

    // Case Instances
    @PostMapping("/cases/{key}/start")
    public Map<String, Object> startCase(@PathVariable("key") String key,
                                        @RequestBody(required = false) Map<String, Object> variables) {
        if (variables == null) variables = new HashMap<>();
        
        CaseInstanceBuilder builder = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(key)
                .variables(variables);
        
        CaseInstance caseInstance = builder.start();
        
        Map<String, Object> result = new HashMap<>();
        result.put("caseInstanceId", caseInstance.getId());
        result.put("caseDefinitionId", caseInstance.getCaseDefinitionId());
        result.put("businessKey", caseInstance.getBusinessKey());
        result.put("startTime", caseInstance.getStartTime());
        return result;
    }

    @GetMapping("/cases")
    public List<Map<String, Object>> getCaseInstances() {
        return cmmnRuntimeService.createCaseInstanceQuery()
                .list()
                .stream()
                .map(ci -> {
                    Map<String, Object> instance = new HashMap<>();
                    instance.put("id", ci.getId());
                    instance.put("caseDefinitionId", ci.getCaseDefinitionId());
                    instance.put("businessKey", ci.getBusinessKey());
                    instance.put("startTime", ci.getStartTime());
                    instance.put("startUserId", ci.getStartUserId());
                    return instance;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/cases/{id}")
    public Map<String, Object> getCaseInstance(@PathVariable("id") String id) {
        CaseInstance ci = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(id)
                .singleResult();
        
        if (ci == null) {
            throw new RuntimeException("Case instance not found");
        }

        Map<String, Object> instance = new HashMap<>();
        instance.put("id", ci.getId());
        instance.put("caseDefinitionId", ci.getCaseDefinitionId());
        instance.put("businessKey", ci.getBusinessKey());
        instance.put("startTime", ci.getStartTime());
        instance.put("startUserId", ci.getStartUserId());
        instance.put("variables", cmmnRuntimeService.getVariables(id));
        return instance;
    }

    @DeleteMapping("/cases/{id}")
    public Map<String, Object> terminateCase(@PathVariable("id") String id) {
        cmmnRuntimeService.terminateCaseInstance(id);
        return Map.of("message", "Case instance terminated successfully");
    }

    // Plan Items
    @GetMapping("/cases/{id}/plan-items")
    public List<Map<String, Object>> getPlanItems(@PathVariable("id") String id) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(id)
                .list()
                .stream()
                .map(pi -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", pi.getId());
                    item.put("name", pi.getName());
                    item.put("state", pi.getState());
                    item.put("planItemDefinitionId", pi.getPlanItemDefinitionId());
                    item.put("planItemDefinitionType", pi.getPlanItemDefinitionType());
                    item.put("startTime", pi.getStartTime());
                    item.put("endTime", null); // PlanItemInstance doesn't have getEndTime method
                    return item;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/plan-items/{id}/start")
    public Map<String, Object> startPlanItem(@PathVariable("id") String id) {
        cmmnRuntimeService.startPlanItemInstance(id);
        return Map.of("message", "Plan item started successfully");
    }

    @PostMapping("/plan-items/{id}/complete")
    public Map<String, Object> completePlanItem(@PathVariable("id") String id) {
        cmmnRuntimeService.triggerPlanItemInstance(id);
        return Map.of("message", "Plan item completed successfully");
    }

    // Case Tasks
    @GetMapping("/tasks")
    public List<Map<String, Object>> getCaseTasks() {
        return cmmnTaskService.createTaskQuery()
                .active()
                .list()
                .stream()
                .map(t -> {
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", t.getId());
                    task.put("name", t.getName());
                    task.put("description", t.getDescription());
                    task.put("assignee", t.getAssignee());
                    task.put("created", t.getCreateTime());
                    task.put("dueDate", t.getDueDate());
                    task.put("caseInstanceId", t.getScopeId());
                    task.put("planItemInstanceId", t.getSubScopeId());
                    return task;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/tasks/{id}")
    public Map<String, Object> getCaseTask(@PathVariable("id") String id) {
        Task task = cmmnTaskService.createTaskQuery()
                .taskId(id)
                .singleResult();
        
        if (task == null) {
            throw new RuntimeException("Task not found");
        }

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("name", task.getName());
        taskMap.put("description", task.getDescription());
        taskMap.put("assignee", task.getAssignee());
        taskMap.put("created", task.getCreateTime());
        taskMap.put("dueDate", task.getDueDate());
        taskMap.put("caseInstanceId", task.getScopeId());
        taskMap.put("planItemInstanceId", task.getSubScopeId());
        taskMap.put("variables", cmmnTaskService.getVariables(id));
        return taskMap;
    }

    @PostMapping("/tasks/{id}/claim")
    public Map<String, Object> claimCaseTask(@PathVariable("id") String id,
                                            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        cmmnTaskService.claim(id, userId);
        return Map.of("message", "Task claimed successfully");
    }

    @PostMapping("/tasks/{id}/complete")
    public Map<String, Object> completeCaseTask(@PathVariable("id") String id,
                                               @RequestBody(required = false) Map<String, Object> variables) {
        if (variables == null) variables = new HashMap<>();
        cmmnTaskService.complete(id, variables);
        return Map.of("message", "Task completed successfully");
    }

    @GetMapping("/tasks/{id}/variables")
    public Map<String, Object> getCaseTaskVariables(@PathVariable("id") String id) {
        return cmmnTaskService.getVariables(id);
    }

    @PostMapping("/tasks/{id}/variables")
    public Map<String, Object> setCaseTaskVariables(@PathVariable("id") String id,
                                                   @RequestBody Map<String, Object> variables) {
        cmmnTaskService.setVariables(id, variables);
        return Map.of("message", "Variables set successfully");
    }

    // History
    @GetMapping("/history/cases")
    public List<Map<String, Object>> getHistoricCaseInstances() {
        return cmmnHistoryService.createHistoricCaseInstanceQuery()
                .finished()
                .list()
                .stream()
                .map(hci -> {
                    Map<String, Object> instance = new HashMap<>();
                    instance.put("id", hci.getId());
                    instance.put("caseDefinitionId", hci.getCaseDefinitionId());
                    instance.put("businessKey", hci.getBusinessKey());
                    instance.put("startTime", hci.getStartTime());
                    instance.put("endTime", hci.getEndTime());
                    instance.put("startUserId", hci.getStartUserId());
                    instance.put("durationInMillis", 0L); // HistoricCaseInstance doesn't have getDurationInMillis method
                    return instance;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/history/cases/{id}")
    public Map<String, Object> getHistoricCaseInstance(@PathVariable("id") String id) {
        HistoricCaseInstance hci = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(id)
                .singleResult();
        
        if (hci == null) {
            throw new RuntimeException("Historic case instance not found");
        }

        Map<String, Object> instance = new HashMap<>();
        instance.put("id", hci.getId());
        instance.put("caseDefinitionId", hci.getCaseDefinitionId());
        instance.put("businessKey", hci.getBusinessKey());
        instance.put("startTime", hci.getStartTime());
        instance.put("endTime", hci.getEndTime());
        instance.put("startUserId", hci.getStartUserId());
        instance.put("durationInMillis", 0L); // HistoricCaseInstance doesn't have getDurationInMillis method
        instance.put("variables", cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(id)
                .list()
                .stream()
                .collect(Collectors.toMap(
                    hvi -> hvi.getVariableName(),
                    hvi -> hvi.getValue()
                )));
        return instance;
    }

    @GetMapping("/history/plan-items/{id}")
    public List<Map<String, Object>> getHistoricPlanItems(@PathVariable("id") String caseInstanceId) {
        return cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .list()
                .stream()
                .map(hpi -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", hpi.getId());
                    item.put("name", hpi.getName());
                    item.put("state", hpi.getState());
                    item.put("planItemDefinitionId", hpi.getPlanItemDefinitionId());
                    item.put("planItemDefinitionType", hpi.getPlanItemDefinitionType());
                    item.put("startTime", null); // HistoricPlanItemInstance doesn't have these methods
                    item.put("endTime", null);
                    item.put("durationInMillis", 0L);
                    return item;
                })
                .collect(Collectors.toList());
    }
}

