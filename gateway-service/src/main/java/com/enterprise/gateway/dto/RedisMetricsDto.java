package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Redis metrics DTO for dashboard
 * Contains Redis connection, memory, and performance metrics
 */
@Data
@Builder
public class RedisMetricsDto {

    /**
     * Current active connections to Redis
     */
    private Integer connections;

    /**
     * Memory used in GB
     */
    private Double memoryUsed;

    /**
     * Total memory available in GB
     */
    private Double memoryTotal;

    /**
     * Cache hit rate percentage (0-100)
     */
    private Double hitRate;

    /**
     * Number of evicted keys
     */
    private Long evictions;

    /**
     * Operations per second
     */
    private Long opsPerSec;
}
