package com.enterprise.process.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Message payload for Process Request creation.
 * Received from Business Service via RabbitMQ.
 *
 * This is a copy of the DTO from business-service for loose coupling.
 * In a larger system, you might use a shared library or protobuf.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID requestId;
    private String processDefinitionKey;
    private String initiatorUserId;
    private String businessKey;
    private Map<String, Object> variables;
    private Instant createdAt;
    private int priority;
}
