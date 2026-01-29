package com.enterprise.process.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.RepositoryService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "http://localhost:3000")
public class BatchController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    // Batch Task Operations
    @PostMapping("/tasks/claim")
    public ResponseEntity<Map<String, Object>> claimTasks(@RequestBody BatchTaskRequest request) {
        List<String> successfulTaskIds = new ArrayList<>();
        List<String> failedTaskIds = new ArrayList<>();

        for (String taskId : request.getTaskIds()) {
            try {
                taskService.claim(taskId, request.getUserId());
                successfulTaskIds.add(taskId);
            } catch (Exception e) {
                failedTaskIds.add(taskId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulTaskIds);
        result.put("failed", failedTaskIds);
        result.put("totalProcessed", request.getTaskIds().size());
        result.put("successCount", successfulTaskIds.size());
        result.put("failureCount", failedTaskIds.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/tasks/complete")
    public ResponseEntity<Map<String, Object>> completeTasks(@RequestBody BatchTaskCompleteRequest request) {
        List<String> successfulTaskIds = new ArrayList<>();
        List<String> failedTaskIds = new ArrayList<>();

        for (String taskId : request.getTaskIds()) {
            try {
                taskService.complete(taskId, request.getVariables());
                successfulTaskIds.add(taskId);
            } catch (Exception e) {
                failedTaskIds.add(taskId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulTaskIds);
        result.put("failed", failedTaskIds);
        result.put("totalProcessed", request.getTaskIds().size());
        result.put("successCount", successfulTaskIds.size());
        result.put("failureCount", failedTaskIds.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/tasks/assign")
    public ResponseEntity<Map<String, Object>> assignTasks(@RequestBody BatchTaskAssignRequest request) {
        List<String> successfulTaskIds = new ArrayList<>();
        List<String> failedTaskIds = new ArrayList<>();

        for (BatchTaskAssignRequest.TaskAssignment assignment : request.getAssignments()) {
            try {
                taskService.setAssignee(assignment.getTaskId(), assignment.getAssignee());
                successfulTaskIds.add(assignment.getTaskId());
            } catch (Exception e) {
                failedTaskIds.add(assignment.getTaskId());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulTaskIds);
        result.put("failed", failedTaskIds);
        result.put("totalProcessed", request.getAssignments().size());
        result.put("successCount", successfulTaskIds.size());
        result.put("failureCount", failedTaskIds.size());

        return ResponseEntity.ok(result);
    }

    // Batch Process Operations
    @PostMapping("/processes/start")
    public ResponseEntity<Map<String, Object>> startProcesses(@RequestBody BatchProcessStartRequest request) {
        List<String> successfulInstanceIds = new ArrayList<>();
        List<String> failedProcessKeys = new ArrayList<>();

        for (BatchProcessStartRequest.ProcessStart processStart : request.getProcesses()) {
            try {
                var processInstance = runtimeService.startProcessInstanceByKey(
                    processStart.getProcessKey(),
                    processStart.getBusinessKey(),
                    processStart.getVariables()
                );
                successfulInstanceIds.add(processInstance.getId());
            } catch (Exception e) {
                failedProcessKeys.add(processStart.getProcessKey());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulInstanceIds);
        result.put("failed", failedProcessKeys);
        result.put("totalProcessed", request.getProcesses().size());
        result.put("successCount", successfulInstanceIds.size());
        result.put("failureCount", failedProcessKeys.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/processes/suspend")
    public ResponseEntity<Map<String, Object>> suspendProcesses(@RequestBody BatchProcessRequest request) {
        List<String> successfulInstanceIds = new ArrayList<>();
        List<String> failedInstanceIds = new ArrayList<>();

        for (String instanceId : request.getProcessInstanceIds()) {
            try {
                runtimeService.suspendProcessInstanceById(instanceId);
                successfulInstanceIds.add(instanceId);
            } catch (Exception e) {
                failedInstanceIds.add(instanceId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulInstanceIds);
        result.put("failed", failedInstanceIds);
        result.put("totalProcessed", request.getProcessInstanceIds().size());
        result.put("successCount", successfulInstanceIds.size());
        result.put("failureCount", failedInstanceIds.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/processes/activate")
    public ResponseEntity<Map<String, Object>> activateProcesses(@RequestBody BatchProcessRequest request) {
        List<String> successfulInstanceIds = new ArrayList<>();
        List<String> failedInstanceIds = new ArrayList<>();

        for (String instanceId : request.getProcessInstanceIds()) {
            try {
                runtimeService.activateProcessInstanceById(instanceId);
                successfulInstanceIds.add(instanceId);
            } catch (Exception e) {
                failedInstanceIds.add(instanceId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulInstanceIds);
        result.put("failed", failedInstanceIds);
        result.put("totalProcessed", request.getProcessInstanceIds().size());
        result.put("successCount", successfulInstanceIds.size());
        result.put("failureCount", failedInstanceIds.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/processes/delete")
    public ResponseEntity<Map<String, Object>> deleteProcesses(@RequestBody BatchProcessDeleteRequest request) {
        List<String> successfulInstanceIds = new ArrayList<>();
        List<String> failedInstanceIds = new ArrayList<>();

        for (String instanceId : request.getProcessInstanceIds()) {
            try {
                runtimeService.deleteProcessInstance(instanceId, request.getDeleteReason());
                successfulInstanceIds.add(instanceId);
            } catch (Exception e) {
                failedInstanceIds.add(instanceId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulInstanceIds);
        result.put("failed", failedInstanceIds);
        result.put("totalProcessed", request.getProcessInstanceIds().size());
        result.put("successCount", successfulInstanceIds.size());
        result.put("failureCount", failedInstanceIds.size());

        return ResponseEntity.ok(result);
    }

    // Get tasks with advanced filtering and pagination
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getTasksWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String taskDefinitionKey,
            @RequestParam(required = false) String createdAfter,
            @RequestParam(required = false) String createdBefore,
            @RequestParam(required = false) String dueAfter,
            @RequestParam(required = false) String dueBefore,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        var query = taskService.createTaskQuery();

        // Apply filters
        if (assignee != null && !assignee.isEmpty()) {
            query.taskAssignee(assignee);
        }
        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            query.processInstanceId(processInstanceId);
        }
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (taskDefinitionKey != null && !taskDefinitionKey.isEmpty()) {
            query.taskDefinitionKey(taskDefinitionKey);
        }
        if (createdAfter != null && !createdAfter.isEmpty()) {
            // query.taskCreatedAfter(parseDate(createdAfter));
        }
        if (createdBefore != null && !createdBefore.isEmpty()) {
            // query.taskCreatedBefore(parseDate(createdBefore));
        }
        if (dueAfter != null && !dueAfter.isEmpty()) {
            // query.taskDueAfter(parseDate(dueAfter));
        }
        if (dueBefore != null && !dueBefore.isEmpty()) {
            // query.taskDueBefore(parseDate(dueBefore));
        }

        // Apply sorting
        switch (sortBy.toLowerCase()) {
            case "name":
                query = "asc".equals(sortOrder) ? query.orderByTaskName().asc() : query.orderByTaskName().desc();
                break;
            case "created":
                query = "asc".equals(sortOrder) ? query.orderByTaskCreateTime().asc() : query.orderByTaskCreateTime().desc();
                break;
            case "due":
                query = "asc".equals(sortOrder) ? query.orderByTaskDueDate().asc() : query.orderByTaskDueDate().desc();
                break;
            default:
                query = query.orderByTaskCreateTime().desc();
        }

        // Get total count
        long totalElements = query.count();

        // Get paginated results
        List<Task> tasks = query
                .listPage(page * size, size);

        List<Map<String, Object>> tasksData = tasks.stream()
                .map(task -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", task.getId());
                    map.put("name", task.getName());
                    map.put("description", task.getDescription());
                    map.put("assignee", task.getAssignee());
                    map.put("created", task.getCreateTime());
                    map.put("dueDate", task.getDueDate());
                    map.put("processInstanceId", task.getProcessInstanceId());
                    map.put("taskDefinitionKey", task.getTaskDefinitionKey());
                    map.put("formKey", task.getFormKey());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", tasksData);
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", totalElements);
        result.put("totalPages", (int) Math.ceil((double) totalElements / size));

        return ResponseEntity.ok(result);
    }

    // Request DTOs
    public static class BatchTaskRequest {
        private List<String> taskIds;
        private String userId;

        // Getters and setters
        public List<String> getTaskIds() { return taskIds; }
        public void setTaskIds(List<String> taskIds) { this.taskIds = taskIds; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class BatchTaskCompleteRequest {
        private List<String> taskIds;
        private Map<String, Object> variables = new HashMap<>();

        // Getters and setters
        public List<String> getTaskIds() { return taskIds; }
        public void setTaskIds(List<String> taskIds) { this.taskIds = taskIds; }
        public Map<String, Object> getVariables() { return variables; }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    }

    public static class BatchTaskAssignRequest {
        private List<TaskAssignment> assignments;

        public List<TaskAssignment> getAssignments() { return assignments; }
        public void setAssignments(List<TaskAssignment> assignments) { this.assignments = assignments; }

        public static class TaskAssignment {
            private String taskId;
            private String assignee;

            public String getTaskId() { return taskId; }
            public void setTaskId(String taskId) { this.taskId = taskId; }
            public String getAssignee() { return assignee; }
            public void setAssignee(String assignee) { this.assignee = assignee; }
        }
    }

    public static class BatchProcessRequest {
        private List<String> processInstanceIds;

        public List<String> getProcessInstanceIds() { return processInstanceIds; }
        public void setProcessInstanceIds(List<String> processInstanceIds) { this.processInstanceIds = processInstanceIds; }
    }

    public static class BatchProcessDeleteRequest extends BatchProcessRequest {
        private String deleteReason;

        public String getDeleteReason() { return deleteReason; }
        public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
    }

    public static class BatchProcessStartRequest {
        private List<ProcessStart> processes;

        public List<ProcessStart> getProcesses() { return processes; }
        public void setProcesses(List<ProcessStart> processes) { this.processes = processes; }

        public static class ProcessStart {
            private String processKey;
            private String businessKey;
            private Map<String, Object> variables = new HashMap<>();

            public String getProcessKey() { return processKey; }
            public void setProcessKey(String processKey) { this.processKey = processKey; }
            public String getBusinessKey() { return businessKey; }
            public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
            public Map<String, Object> getVariables() { return variables; }
            public void setVariables(Map<String, Object> variables) { this.variables = variables; }
        }
    }
}

