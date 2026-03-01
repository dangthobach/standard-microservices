package com.enterprise.process.controller;

import com.enterprise.common.constant.ApiConstants;
import com.enterprise.common.dto.ApiResponse;
import com.enterprise.process.dto.CompleteTaskRequest;
import com.enterprise.process.dto.TaskResponse;
import com.enterprise.process.service.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Manage user tasks")
public class TaskController {

    private final WorkflowTaskService workflowTaskService;

    @GetMapping
    @Operation(summary = "Get tasks", description = "Get tasks assigned to a user or candidate group")
    // @PreAuthorize("hasAuthority('task:read')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String candidateGroup) {

        List<TaskResponse> tasks = workflowTaskService.getTasks(assignee, candidateGroup);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a task", description = "Complete a task with variables")
    // @PreAuthorize("hasAuthority('task:write')")
    public ResponseEntity<ApiResponse<Void>> completeTask(
            @PathVariable String id,
            @RequestBody(required = false) CompleteTaskRequest request) {

        workflowTaskService.completeTask(id, request != null ? request.getVariables() : null);
        return ResponseEntity.ok(ApiResponse.success("Task completed successfully", null));
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "Claim a task", description = "Claim a task for a specific user")
    // @PreAuthorize("hasAuthority('task:write')")
    public ResponseEntity<ApiResponse<Void>> claimTask(
            @PathVariable String id,
            @RequestParam String userId) {

        workflowTaskService.claimTask(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Task claimed successfully", null));
    }
}
