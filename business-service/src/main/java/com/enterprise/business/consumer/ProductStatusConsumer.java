package com.enterprise.business.consumer;

import com.enterprise.business.config.RabbitMQConfig;
import com.enterprise.business.proto.ProductStatusProto;
import com.enterprise.business.repository.ProductRepository;
import com.enterprise.business.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * RabbitMQ Consumer for Product Status changes.
 * Listens to events from Process Service and updates local DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStatusConsumer {

    private final ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCT_STATUS) // Need to define this queue
    @Transactional
    public void handleProductStatusChange(ProductStatusProto.ProductStatusChangedEvent event) {
        log.info("Received product status update: productId={}, newStatus={}", event.getProductId(), event.getNewStatus());

        try {
            UUID productId = UUID.fromString(event.getProductId());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            product.setStatus(event.getNewStatus());
            product.setProcessInstanceId(event.getProcessInstanceId());

            if ("ACTIVE".equals(event.getNewStatus())) {
                product.setActive(true);
            }

            productRepository.save(product);
            log.info("Updated product status in DB successfully.");

        } catch (Exception e) {
            log.error("Failed to update product status: {}", e.getMessage(), e);
            // Throw to retry/DLQ
            throw e;
        }
    }
}
