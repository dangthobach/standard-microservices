package com.enterprise.process.bpmn;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.Task;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.enterprise.process.websocket.NotificationService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flow")
public class FlowController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    @Autowired
    private NotificationService notificationService;

    public FlowController(RuntimeService runtimeService, TaskService taskService,
            RepositoryService repositoryService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
    }

    // Process Management
    @GetMapping("/processes")
    public List<Map<String, Object>> getProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .active()
                .list()
                .stream()
                .map(pd -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", pd.getId());
                    map.put("key", pd.getKey());
                    map.put("name", pd.getName());
                    map.put("version", pd.getVersion());
                    map.put("deploymentId", pd.getDeploymentId());
                    map.put("resourceName", pd.getResourceName());
                    map.put("dgrmResourceName", pd.getDiagramResourceName());
                    map.put("description", pd.getDescription());
                    map.put("hasStartFormKey", pd.hasStartFormKey());
                    map.put("hasGraphicalNotation", pd.hasGraphicalNotation());
                    map.put("suspensionState", pd.isSuspended() ? 2 : 1);
                    map.put("tenantId", pd.getTenantId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/processes/{key}/start")
    public Map<String, Object> startProcess(@PathVariable("key") String key,
            @RequestBody(required = false) Map<String, Object> vars) {
        if (vars == null)
            vars = new HashMap<>();
        var pi = runtimeService.startProcessInstanceByKey(key, vars);

        // Send real-time notification
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(pi.getProcessDefinitionId())
                    .singleResult();
            String processName = processDefinition != null ? processDefinition.getName() : key;

            notificationService.sendProcessStatusNotification(
                    pi.getId(),
                    processName,
                    "STARTED");

            notificationService.sendGlobalNotification(
                    String.format("Process '%s' has been started", processName),
                    "success");
        } catch (Exception e) {
            // Log error but don't fail the process start
            System.err.println("Failed to send notification: " + e.getMessage());
        }

        return Map.of("instanceId", pi.getId(), "definitionId", pi.getProcessDefinitionId());
    }

    @GetMapping("/processes/{key}/bpmn")
    public String getProcessBpmn(@PathVariable("key") String key) {
        var processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();

        if (processDefinition == null) {
            throw new RuntimeException("Process definition not found");
        }

        var resourceStream = repositoryService.getResourceAsStream(
                processDefinition.getDeploymentId(),
                processDefinition.getResourceName());

        try {
            return new String(resourceStream.readAllBytes(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read BPMN resource", e);
        }
    }

    @GetMapping("/instances")
    public List<Map<String, Object>> getProcessInstances() {
        return runtimeService.createProcessInstanceQuery()
                .active()
                .list()
                .stream()
                .map(pi -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", pi.getId());
                    map.put("processDefinitionId", pi.getProcessDefinitionId());
                    map.put("processDefinitionName", pi.getProcessDefinitionName());
                    map.put("businessKey", pi.getBusinessKey());
                    map.put("startTime", pi.getStartTime());
                    map.put("startUserId", pi.getStartUserId());
                    map.put("startActivityId", pi.getActivityId());
                    map.put("tenantId", pi.getTenantId());
                    map.put("state", "active");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/processes/instances")
    public List<Map<String, Object>> getProcessInstancesByKey(
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey) {
        var query = runtimeService.createProcessInstanceQuery().active();

        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }

        return query.list()
                .stream()
                .map(pi -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", pi.getId());
                    map.put("processDefinitionId", pi.getProcessDefinitionId());
                    map.put("processDefinitionKey", pi.getProcessDefinitionKey());
                    map.put("processDefinitionName", pi.getProcessDefinitionName());
                    map.put("businessKey", pi.getBusinessKey());
                    map.put("startTime", pi.getStartTime());
                    map.put("startUserId", pi.getStartUserId());
                    map.put("startActivityId", pi.getActivityId());
                    map.put("tenantId", pi.getTenantId());
                    map.put("suspended", pi.isSuspended());
                    map.put("state", pi.isSuspended() ? "suspended" : "active");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/instances/{id}")
    public Map<String, Object> getProcessInstance(@PathVariable("id") String id) {
        var pi = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
        if (pi == null) {
            throw new RuntimeException("Process instance not found");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", pi.getId());
        map.put("processDefinitionId", pi.getProcessDefinitionId());
        map.put("processDefinitionName", pi.getProcessDefinitionName());
        map.put("businessKey", pi.getBusinessKey());
        map.put("startTime", pi.getStartTime());
        map.put("startUserId", pi.getStartUserId());
        map.put("startActivityId", pi.getActivityId());
        map.put("tenantId", pi.getTenantId());
        map.put("state", "active");
        return map;
    }

    // Task Management
    @GetMapping("/tasks")
    public List<Map<String, Object>> getTasks() {
        return taskService.createTaskQuery().list().stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("name", t.getName());
                    map.put("description", t.getDescription());
                    map.put("assignee", t.getAssignee());
                    map.put("created", t.getCreateTime());
                    map.put("dueDate", t.getDueDate());
                    map.put("processInstanceId", t.getProcessInstanceId());
                    map.put("taskDefinitionKey", t.getTaskDefinitionKey());
                    map.put("formKey", t.getFormKey());
                    map.put("priority", t.getPriority());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get tasks for the current authenticated user.
     * Returns tasks that are either:
     * - Assigned to the user
     * - In candidate groups the user belongs to
     * - Unassigned but claimable
     */
    @GetMapping("/tasks/my")
    public List<Map<String, Object>> getMyTasks(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        
        List<Task> tasks = new ArrayList<>();
        
        // If userId is provided, get assigned tasks
        if (userId != null && !userId.isEmpty()) {
            tasks.addAll(taskService.createTaskQuery()
                    .taskAssignee(userId)
                    .list());
            
            // Also get candidate tasks (unassigned but claimable)
            tasks.addAll(taskService.createTaskQuery()
                    .taskCandidateUser(userId)
                    .list());
        }
        
        // If roles are provided, get candidate group tasks
        if (userRoles != null && !userRoles.isEmpty()) {
            List<String> roles = Arrays.asList(userRoles.split(","));
            for (String role : roles) {
                tasks.addAll(taskService.createTaskQuery()
                        .taskCandidateGroup(role.trim())
                        .list());
            }
        }
        
        // Remove duplicates by taskId
        Map<String, Task> uniqueTasks = new LinkedHashMap<>();
        for (Task task : tasks) {
            uniqueTasks.putIfAbsent(task.getId(), task);
        }
        
        return uniqueTasks.values().stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("name", t.getName());
                    map.put("description", t.getDescription());
                    map.put("assignee", t.getAssignee());
                    map.put("createTime", t.getCreateTime());
                    map.put("dueDate", t.getDueDate());
                    map.put("processInstanceId", t.getProcessInstanceId());
                    map.put("processDefinitionId", t.getProcessDefinitionId());
                    map.put("taskDefinitionKey", t.getTaskDefinitionKey());
                    map.put("formKey", t.getFormKey());
                    map.put("priority", t.getPriority());
                    map.put("owner", t.getOwner());
                    map.put("delegationState", t.getDelegationState());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/tasks/{id}")
    public Map<String, Object> getTask(@PathVariable("id") String id) {
        var task = taskService.createTaskQuery().taskId(id).singleResult();
        if (task == null) {
            throw new RuntimeException("Task not found");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("name", task.getName());
        map.put("description", task.getDescription());
        map.put("assignee", task.getAssignee());
        map.put("created", task.getCreateTime());
        map.put("dueDate", task.getDueDate());
        map.put("processInstanceId", task.getProcessInstanceId());
        map.put("processDefinitionId", task.getProcessDefinitionId());
        map.put("taskDefinitionKey", task.getTaskDefinitionKey());
        map.put("formKey", task.getFormKey());
        map.put("priority", task.getPriority());
        map.put("owner", task.getOwner());
        map.put("delegationState", task.getDelegationState());
        return map;
    }

    @PostMapping("/tasks/{id}/claim")
    public Map<String, String> claimTask(@PathVariable("id") String id, @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        taskService.claim(id, userId);

        // Send real-time notification
        try {
            Task task = taskService.createTaskQuery().taskId(id).singleResult();
            if (task != null) {
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(task.getProcessDefinitionId())
                        .singleResult();
                String processName = processDefinition != null ? processDefinition.getName() : "Unknown Process";

                notificationService.sendTaskAssignedNotification(
                        userId,
                        id,
                        task.getName(),
                        processName);

                notificationService.sendGlobalNotification(
                        String.format("Task '%s' has been claimed by %s", task.getName(), userId),
                        "info");
            }
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }

        return Map.of("status", "claimed", "taskId", id, "userId", userId);
    }

    @PostMapping("/tasks/{id}/unclaim")
    public Map<String, String> unclaimTask(@PathVariable("id") String id) {
        taskService.unclaim(id);
        return Map.of("status", "unclaimed", "taskId", id);
    }

    @PostMapping("/tasks/{id}/delegate")
    public Map<String, String> delegateTask(@PathVariable("id") String id, @RequestBody Map<String, String> request) {
        String delegateUserId = request.get("userId");
        String comment = request.get("comment");
        
        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        if (task == null) {
            throw new RuntimeException("Task not found");
        }
        
        // Add comment if provided
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(id, task.getProcessInstanceId(), "delegation", 
                    "Delegated to " + delegateUserId + ": " + comment);
        }
        
        // Delegate the task
        taskService.delegateTask(id, delegateUserId);
        
        // Send notification
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            String processName = processDefinition != null ? processDefinition.getName() : "Unknown Process";

            notificationService.sendTaskAssignedNotification(
                    delegateUserId,
                    id,
                    task.getName(),
                    processName);

            notificationService.sendGlobalNotification(
                    String.format("Task '%s' has been delegated to %s", task.getName(), delegateUserId),
                    "info");
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }

        return Map.of("status", "delegated", "taskId", id, "delegatedTo", delegateUserId);
    }

    @PostMapping("/tasks/{id}/resolve")
    public Map<String, String> resolveTask(@PathVariable("id") String id) {
        taskService.resolveTask(id);
        return Map.of("status", "resolved", "taskId", id);
    }

    @PostMapping("/tasks/{id}/complete")
    public Map<String, String> completeTask(@PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> variables = new HashMap<>();
        if (request != null && request.containsKey("variables")) {
            Object variablesObj = request.get("variables");
            if (variablesObj instanceof Map) {
                variables = (Map<String, Object>) variablesObj;
            }
        }

        // Get task info before completion for notification
        Task task = null;
        String processName = "Unknown Process";
        try {
            task = taskService.createTaskQuery().taskId(id).singleResult();
            if (task != null) {
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(task.getProcessDefinitionId())
                        .singleResult();
                processName = processDefinition != null ? processDefinition.getName() : processName;
            }
        } catch (Exception e) {
            System.err.println("Failed to get task info: " + e.getMessage());
        }

        taskService.complete(id, variables);

        // Send real-time notification
        try {
            if (task != null) {
                notificationService.sendGlobalNotification(
                        String.format("Task '%s' has been completed in process '%s'", task.getName(), processName),
                        "success");
            }
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }

        return Map.of("status", "completed", "taskId", id);
    }

    @GetMapping("/tasks/{id}/variables")
    public Map<String, Object> getTaskVariables(@PathVariable("id") String id) {
        return taskService.getVariables(id);
    }

    @PostMapping("/tasks/{id}/variables")
    public Map<String, String> setTaskVariables(@PathVariable("id") String id,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        taskService.setVariables(id, variables);
        return Map.of("status", "variables_set", "taskId", id);
    }

    // History
    @GetMapping("/history/processes/{id}")
    public List<Map<String, Object>> getProcessHistory(@PathVariable("id") String id) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id)
                .list()
                .stream()
                .map(hi -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", hi.getId());
                    map.put("processDefinitionId", hi.getProcessDefinitionId());
                    map.put("processDefinitionName", hi.getProcessDefinitionName());
                    map.put("businessKey", hi.getBusinessKey());
                    map.put("startTime", hi.getStartTime());
                    map.put("endTime", hi.getEndTime());
                    map.put("durationInMillis", hi.getDurationInMillis());
                    map.put("startUserId", hi.getStartUserId());
                    map.put("startActivityId", hi.getStartActivityId());
                    map.put("endActivityId", hi.getEndActivityId());
                    map.put("deleteReason", hi.getDeleteReason());
                    map.put("tenantId", hi.getTenantId());
                    map.put("state", hi.getEndTime() != null ? "completed" : "active");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/history/tasks")
    public List<Map<String, Object>> getTaskHistory(
            @RequestParam(value = "processInstanceId", required = false) String processInstanceId) {
        var query = historyService.createHistoricTaskInstanceQuery();

        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            query.processInstanceId(processInstanceId);
        }

        return query.list()
                .stream()
                .map(hi -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", hi.getId());
                    map.put("name", hi.getName());
                    map.put("description", hi.getDescription());
                    map.put("assignee", hi.getAssignee());
                    map.put("startTime", hi.getCreateTime()); // Use getCreateTime() instead of deprecated
                                                              // getStartTime()
                    map.put("endTime", hi.getEndTime());
                    map.put("durationInMillis", hi.getDurationInMillis());
                    map.put("processInstanceId", hi.getProcessInstanceId());
                    map.put("taskDefinitionKey", hi.getTaskDefinitionKey());
                    map.put("deleteReason", hi.getDeleteReason());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/history/tasks/{id}")
    public List<Map<String, Object>> getTaskHistoryById(@PathVariable("id") String id) {
        return historyService.createHistoricTaskInstanceQuery()
                .taskId(id)
                .list()
                .stream()
                .map(hi -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", hi.getId());
                    map.put("name", hi.getName());
                    map.put("description", hi.getDescription());
                    map.put("assignee", hi.getAssignee());
                    map.put("startTime", hi.getCreateTime());
                    map.put("endTime", hi.getEndTime());
                    map.put("durationInMillis", hi.getDurationInMillis());
                    map.put("processInstanceId", hi.getProcessInstanceId());
                    map.put("taskDefinitionKey", hi.getTaskDefinitionKey());
                    map.put("deleteReason", hi.getDeleteReason());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // Process Variables
    @GetMapping("/processes/instances/{id}/variables")
    public Map<String, Object> getProcessVariables(@PathVariable("id") String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    @PutMapping("/processes/instances/{id}/variables/{name}")
    public Map<String, String> updateProcessVariable(@PathVariable("id") String processInstanceId,
            @PathVariable("name") String variableName,
            @RequestBody Map<String, Object> request) {
        Object value = request.get("value");
        runtimeService.setVariable(processInstanceId, variableName, value);
        return Map.of("status", "updated", "variableName", variableName);
    }

    // Process Deployment
    @PostMapping("/deploy")
    public Map<String, Object> deployProcess(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "processKey", required = false) String processKey,
            @RequestParam(value = "processName", required = false) String processName) {
        try {
            String bpmnXml = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Deploy the process
            Deployment deployment = repositoryService.createDeployment()
                    .addString(file.getOriginalFilename(), bpmnXml)
                    .name(processName != null ? processName : "Process Deployment")
                    .deploy();

            // Get the deployed process definition
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            Map<String, Object> result = new HashMap<>();
            result.put("deploymentId", deployment.getId());
            result.put("processDefinitionId", processDefinition.getId());
            result.put("processKey", processDefinition.getKey());
            result.put("processName", processDefinition.getName());
            result.put("version", processDefinition.getVersion());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deploy process: " + e.getMessage(), e);
        }
    }

    // Delete Process Definition
    @DeleteMapping("/processes/{id}")
    public Map<String, String> deleteProcess(@PathVariable("id") String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (processDefinition == null) {
                throw new RuntimeException("Process definition not found");
            }

            // Check if there are running instances
            long runningInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionId(processDefinitionId)
                    .count();

            if (runningInstances > 0) {
                throw new RuntimeException("Cannot delete process with running instances");
            }

            // Delete the deployment (cascade will delete process definition)
            repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);

            return Map.of("message", "Process definition deleted successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete process: " + e.getMessage(), e);
        }
    }
}
