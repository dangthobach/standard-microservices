package com.enterprise.business.service;

import com.enterprise.business.config.RabbitMQConfig;
import com.enterprise.business.proto.ProcessRequestProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RabbitMQ Producer for sending process requests to Process Management Service.
 * Uses Protobuf for message serialization.
 *
 * Usage:
 * <pre>
 * requestProducer.sendProcessRequest(
 *     "expense-approval-process",
 *     "user123",
 *     "INV-2024-001",
 *     Map.of("amount", "1500", "department", "Engineering"),
 *     5
 * );
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send a process request to the Process Management Service.
     *
     * @param processDefinitionKey The key of the process to start (e.g., "expense-approval-process").
     * @param initiatorUserId      The ID of the user initiating the request.
     * @param businessKey          A unique business key for correlation.
     * @param variables            Variables to pass to the process (values will be converted to String).
     * @param priority             Priority level (1-10).
     * @return The generated request ID.
     */
    public UUID sendProcessRequest(
            String processDefinitionKey,
            String initiatorUserId,
            String businessKey,
            Map<String, Object> variables,
            int priority
    ) {
        UUID requestId = UUID.randomUUID();

        // Convert variables to Map<String, String> for Protobuf
        Map<String, String> stringVariables = variables.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.valueOf(e.getValue())
                ));

        // Build Protobuf message
        ProcessRequestProto.ProcessRequest message = ProcessRequestProto.ProcessRequest.newBuilder()
                .setRequestId(requestId.toString())
                .setProcessDefinitionKey(processDefinitionKey)
                .setInitiatorUserId(initiatorUserId)
                .setBusinessKey(businessKey)
                .putAllVariables(stringVariables)
                .setCreatedAt(Instant.now().toString())
                .setPriority(priority)
                .build();

        log.info("Sending process request (Protobuf): requestId={}, processKey={}, businessKey={}",
                requestId, processDefinitionKey, businessKey);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_PROCESS_REQUEST,
                message
        );

        log.debug("Process request sent successfully via Protobuf");

        return requestId;
    }

    /**
     * Convenience method with default priority (5).
     */
    public UUID sendProcessRequest(
            String processDefinitionKey,
            String initiatorUserId,
            String businessKey,
            Map<String, Object> variables
    ) {
        return sendProcessRequest(processDefinitionKey, initiatorUserId, businessKey, variables, 5);
    }
}
