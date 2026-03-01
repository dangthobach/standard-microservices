package com.enterprise.process.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private String id;
    private String name;
    private String assignee;
    private Date createTime;
    private String processInstanceId;
    private String executionId;
    private String taskDefinitionKey;
    private Map<String, Object> variables;
}
