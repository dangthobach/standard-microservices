package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Slow endpoint DTO for dashboard
 * Contains performance metrics for slow API endpoints
 */
@Data
@Builder
public class SlowEndpointDto {

    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    private String method;

    /**
     * API path (e.g., "/api/reports/generate")
     */
    private String path;

    /**
     * Average latency in milliseconds
     */
    private Long avgLatency;

    /**
     * 95th percentile latency in milliseconds
     */
    private Long p95Latency;

    /**
     * Total number of calls
     */
    private Long calls;
}
