package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Service health status DTO for dashboard
 * Contains health metrics for individual microservices
 */
@Data
@Builder
public class ServiceHealthDto {

    /**
     * Service name (e.g., "Gateway Service", "IAM Service")
     */
    private String name;

    /**
     * Health status: "healthy", "warning", "critical", "unknown"
     */
    private String status;

    /**
     * CPU usage percentage (0-100)
     */
    private Double cpu;

    /**
     * Memory usage percentage (0-100)
     */
    private Double memory;

    /**
     * Uptime in human-readable format (e.g., "15d 2h")
     */
    private String uptime;

    /**
     * Total requests handled
     */
    private Long requests;

    /**
     * Total errors encountered
     */
    private Long errors;
}
