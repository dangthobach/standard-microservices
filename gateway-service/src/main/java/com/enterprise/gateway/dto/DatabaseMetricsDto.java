package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Database metrics DTO for dashboard (Distributed Monitoring)
 * Contains connection pool and query metrics from ALL microservices
 *
 * Each service with a database will report its metrics independently.
 * Dashboard aggregates all service databases for unified monitoring.
 */
@Data
@Builder
public class DatabaseMetricsDto {

    /**
     * Service name owning this database (e.g., "iam-service", "business-service")
     * Used to identify which microservice's database this is
     */
    private String serviceName;

    /**
     * Database name or identifier (e.g., "Primary DB", "Cache DB")
     * Default: "{serviceName} Database"
     */
    private String name;

    /**
     * Current active connections
     */
    private Long connections;

    /**
     * Maximum allowed connections (HikariCP pool size)
     */
    private Long maxConnections;

    /**
     * Active connections (currently in use)
     */
    private Long activeConnections;

    /**
     * Idle connections (available in pool)
     */
    private Long idleConnections;

    /**
     * Connection pool usage percentage (0-100)
     */
    private Double poolUsage;

    /**
     * Currently running queries
     */
    private Long activeQueries;

    /**
     * Number of slow queries detected
     */
    private Long slowQueries;

    /**
     * Cache hit rate percentage (0-100)
     */
    private Double cacheHitRate;
}
