package com.enterprise.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for RabbitMQ Queue Statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatsDTO {
    private String queueName;
    private long messageCount;
    private long consumerCount;
    private String state;
    private long messagesReady;
    private long messagesUnacknowledged;
}
