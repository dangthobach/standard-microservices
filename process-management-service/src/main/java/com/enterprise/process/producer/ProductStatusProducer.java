package com.enterprise.process.producer;

import com.enterprise.process.config.RabbitMQConfig;
import com.enterprise.process.proto.ProductStatusProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Producer to send Product Status Change events to RabbitMQ.
 * Business Service will consume these to update the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStatusProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendStatusChange(String productId, String newStatus, String processInstanceId) {
        log.info("Sending product status change: productId={}, status={}, processInstanceId={}",
                productId, newStatus, processInstanceId);

        ProductStatusProto.ProductStatusChangedEvent event = ProductStatusProto.ProductStatusChangedEvent.newBuilder()
                .setProductId(productId)
                .setNewStatus(newStatus)
                .setProcessInstanceId(processInstanceId)
                .setTimestamp(Instant.now().toString())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_PRODUCT_STATUS,
                event
        );
    }
}
