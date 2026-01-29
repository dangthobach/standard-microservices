package com.enterprise.process.consumer;

import com.enterprise.process.config.RabbitMQConfig;
import com.enterprise.process.proto.ProcessRequestProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ Consumer for processing incoming requests.
 * Uses Protobuf for message deserialization.
 *
 * Listens to the process.request.queue and starts Flowable process instances.
 * If processing fails, the message is automatically sent to the DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestConsumer {

    private final RuntimeService runtimeService;

    /**
     * Process incoming Protobuf request messages.
     *
     * @param request The process request message from Business Service.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleProcessRequest(ProcessRequestProto.ProcessRequest request) {
        log.info("Received process request (Protobuf): requestId={}, processKey={}, businessKey={}",
                request.getRequestId(), request.getProcessDefinitionKey(), request.getBusinessKey());

        try {
            // Prepare variables for the process
            Map<String, Object> variables = new HashMap<>();

            // Add all Protobuf variables (they are already Map<String, String>)
            variables.putAll(request.getVariablesMap());

            // Add metadata variables
            variables.put("requestId", request.getRequestId());
            variables.put("initiator", request.getInitiatorUserId());
            variables.put("priority", request.getPriority());
            variables.put("createdAt", request.getCreatedAt());

            // Start the process instance using Flowable RuntimeService
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    request.getProcessDefinitionKey(),
                    request.getBusinessKey(),
                    variables
            );

            log.info("Process instance started successfully: processInstanceId={}, requestId={}, processKey={}",
                    processInstance.getId(), request.getRequestId(), request.getProcessDefinitionKey());

        } catch (Exception e) {
            log.error("Failed to process request: requestId={}, processKey={}, error={}",
                    request.getRequestId(), request.getProcessDefinitionKey(), e.getMessage(), e);

            // Re-throw to trigger DLQ routing
            throw new RuntimeException("Failed to start process for request: " + request.getRequestId(), e);
        }
    }
}
