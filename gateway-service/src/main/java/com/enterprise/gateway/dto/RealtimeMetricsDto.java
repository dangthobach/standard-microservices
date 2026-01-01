package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Real-time metrics DTO for dashboard
 * Contains CCU, RPS, error rate, and average latency
 */
@Data
@Builder
public class RealtimeMetricsDto {

    /**
     * Current Concurrent Users (Active sessions)
     */
    private Long ccu;

    /**
     * Requests Per Second (Current rate)
     */
    private Long rps;

    /**
     * Error Rate (0.0 to 1.0)
     */
    private Double errorRate;

    /**
     * Average Latency in milliseconds
     */
    private Double avgLatency;
}
