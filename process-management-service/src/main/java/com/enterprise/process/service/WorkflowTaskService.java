package com.enterprise.process.service;

import com.enterprise.process.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTaskService {

    private final TaskService taskService;

    /**
     * Get tasks assigned to a user or candidate group
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(String assignee, String candidateGroup) {
        List<Task> tasks;

        if (assignee != null) {
            tasks = taskService.createTaskQuery()
                    .taskAssignee(assignee)
                    .orderByTaskCreateTime().desc()
                    .list();
        } else if (candidateGroup != null) {
            tasks = taskService.createTaskQuery()
                    .taskCandidateGroup(candidateGroup)
                    .orderByTaskCreateTime().desc()
                    .list();
        } else {
            // Admin or general query (limit to avoid heavy load)
            tasks = taskService.createTaskQuery()
                    .orderByTaskCreateTime().desc()
                    .listPage(0, 50);
        }

        return tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Complete a task
     */
    @Transactional
    public void completeTask(String taskId, Map<String, Object> variables) {
        log.info("Completing task: id={}, variables={}", taskId, variables);
        taskService.complete(taskId, variables);
    }

    /**
     * Claim a task
     */
    @Transactional
    public void claimTask(String taskId, String userId) {
        log.info("Claiming task: id={}, userId={}", taskId, userId);
        taskService.claim(taskId, userId);
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .assignee(task.getAssignee())
                .createTime(task.getCreateTime())
                .processInstanceId(task.getProcessInstanceId())
                .executionId(task.getExecutionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .variables(taskService.getVariables(task.getId()))
                .build();
    }
}
