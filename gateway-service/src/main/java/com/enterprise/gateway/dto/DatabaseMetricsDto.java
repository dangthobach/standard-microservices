package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Database metrics DTO for dashboard
 * Contains connection pool and query metrics
 */
@Data
@Builder
public class DatabaseMetricsDto {

    /**
     * Database name or identifier (e.g., "Primary DB", "Cache DB")
     */
    private String name;

    /**
     * Current active connections
     */
    private Integer connections;

    /**
     * Maximum allowed connections
     */
    private Integer maxConnections;

    /**
     * Currently running queries
     */
    private Integer activeQueries;

    /**
     * Number of slow queries detected
     */
    private Integer slowQueries;

    /**
     * Cache hit rate percentage (0-100)
     */
    private Double cacheHitRate;
}
