package com.enterprise.business.controller;

import com.enterprise.business.dto.QueueStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RabbitMQ Monitoring Controller
 * Provides endpoints to monitor queue status and message flow
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/rabbitmq")
@RequiredArgsConstructor
public class RabbitMQMonitoringController {

    private final AmqpAdmin amqpAdmin;

    /**
     * Get statistics for all known queues
     */
    @GetMapping("/queues")
    public ResponseEntity<List<QueueStatsDTO>> getQueueStats() {
        log.info("Fetching RabbitMQ queue statistics");
        
        List<QueueStatsDTO> stats = new ArrayList<>();
        
        // List of queues to monitor
        String[] queues = {
            "product.status.queue",
            "process.request.queue",
            "process.request.dlq"
        };
        
        for (String queueName : queues) {
            try {
                QueueInformation info = amqpAdmin.getQueueInfo(queueName);
                if (info != null) {
                    QueueStatsDTO dto = new QueueStatsDTO();
                    dto.setQueueName(queueName);
                    dto.setMessageCount(info.getMessageCount());
                    dto.setConsumerCount(info.getConsumerCount());
                    dto.setState("running");
                    
                    // Additional properties if available
                    if (info.getMessageCount() > 0) {
                        dto.setMessagesReady(info.getMessageCount());
                    }
                    
                    stats.add(dto);
                } else {
                    // Queue doesn't exist
                    QueueStatsDTO dto = new QueueStatsDTO();
                    dto.setQueueName(queueName);
                    dto.setMessageCount(0);
                    dto.setConsumerCount(0);
                    dto.setState("not_found");
                    stats.add(dto);
                }
            } catch (Exception e) {
                log.error("Failed to get stats for queue: {}", queueName, e);
                QueueStatsDTO dto = new QueueStatsDTO();
                dto.setQueueName(queueName);
                dto.setState("error");
                stats.add(dto);
            }
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get queue info for a specific queue
     */
    @GetMapping("/queue/product-status")
    public ResponseEntity<QueueStatsDTO> getProductStatusQueueInfo() {
        String queueName = "product.status.queue";
        
        try {
            QueueInformation info = amqpAdmin.getQueueInfo(queueName);
            if (info != null) {
                QueueStatsDTO dto = new QueueStatsDTO();
                dto.setQueueName(queueName);
                dto.setMessageCount(info.getMessageCount());
                dto.setConsumerCount(info.getConsumerCount());
                dto.setState("running");
                dto.setMessagesReady(info.getMessageCount());
                
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to get queue info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint for RabbitMQ
     */
    @GetMapping("/health")
    public ResponseEntity<String> checkRabbitMQHealth() {
        try {
            // Try to get info for a known queue
            QueueInformation info = amqpAdmin.getQueueInfo("product.status.queue");
            if (info != null) {
                return ResponseEntity.ok("RabbitMQ is healthy");
            } else {
                return ResponseEntity.ok("RabbitMQ connected but queue not found");
            }
        } catch (Exception e) {
            log.error("RabbitMQ health check failed", e);
            return ResponseEntity.status(503).body("RabbitMQ is not available");
        }
    }
}
