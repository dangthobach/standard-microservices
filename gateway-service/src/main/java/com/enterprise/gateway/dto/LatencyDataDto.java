package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Latency percentile data DTO for dashboard heatmap
 * Contains P50, P95, P99 latency metrics per service
 */
@Data
@Builder
public class LatencyDataDto {

    /**
     * Service name (e.g., "Gateway", "Auth", "Business")
     */
    private String service;

    /**
     * 50th percentile latency in milliseconds
     */
    private Long p50;

    /**
     * 95th percentile latency in milliseconds
     */
    private Long p95;

    /**
     * 99th percentile latency in milliseconds
     */
    private Long p99;
}
