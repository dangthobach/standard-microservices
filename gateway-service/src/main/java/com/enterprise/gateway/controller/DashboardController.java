package com.enterprise.gateway.controller;

import com.enterprise.common.cqrs.QueryBus;
import com.enterprise.common.dto.ApiResponse;
import com.enterprise.gateway.dto.*;
import com.enterprise.gateway.query.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dashboard API Controller
 * Provides real-time metrics and monitoring data for the dashboard
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard metrics and monitoring APIs")
public class DashboardController {

    private final QueryBus queryBus;

    /**
     * Get real-time metrics (CCU, RPS, Error Rate, Avg Latency)
     */
    @GetMapping("/realtime")
    @Operation(summary = "Get real-time metrics", description = "Returns current CCU, RPS, error rate, and average latency")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RealtimeMetricsDto>> getRealtimeMetrics() {
        log.debug("Getting real-time metrics");
        RealtimeMetricsDto metrics = queryBus.execute(new GetRealtimeMetricsQuery());
        return ResponseEntity.ok(ApiResponse.success("Real-time metrics retrieved successfully", metrics));
    }

    /**
     * Get service health status
     */
    @GetMapping("/services")
    @Operation(summary = "Get service health", description = "Returns health status of all microservices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ServiceHealthDto>>> getServiceHealth() {
        log.debug("Getting service health");
        List<ServiceHealthDto> services = queryBus.execute(new GetServiceHealthQuery());
        return ResponseEntity.ok(ApiResponse.success("Service health retrieved successfully", services));
    }

    /**
     * Get traffic history for charts (last 24 hours)
     */
    @GetMapping("/traffic")
    @Operation(summary = "Get traffic history", description = "Returns traffic data for the last 24 hours in 5-minute intervals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TrafficDataDto>>> getTrafficHistory() {
        log.debug("Getting traffic history");
        List<TrafficDataDto> traffic = queryBus.execute(new GetTrafficHistoryQuery());
        return ResponseEntity.ok(ApiResponse.success("Traffic history retrieved successfully", traffic));
    }

    /**
     * Get database metrics (connections, queries, cache hit rate)
     */
    @GetMapping("/database")
    @Operation(summary = "Get database metrics", description = "Returns database connection pool and query statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DatabaseMetricsDto>>> getDatabaseMetrics() {
        log.debug("Getting database metrics");
        List<DatabaseMetricsDto> metrics = queryBus.execute(new GetDatabaseMetricsQuery());
        return ResponseEntity.ok(ApiResponse.success("Database metrics retrieved successfully", metrics));
    }

    /**
     * Get latency percentiles by service
     */
    @GetMapping("/latency")
    @Operation(summary = "Get latency heatmap", description = "Returns P50, P95, P99 latency by service")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LatencyDataDto>>> getLatencyMetrics() {
        log.debug("Getting latency metrics");
        List<LatencyDataDto> latency = queryBus.execute(new GetLatencyMetricsQuery());
        return ResponseEntity.ok(ApiResponse.success("Latency metrics retrieved successfully", latency));
    }

    /**
     * Get Redis metrics (memory, connections, hit rate)
     */
    @GetMapping("/redis")
    @Operation(summary = "Get Redis metrics", description = "Returns Redis memory usage, connections, and performance metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RedisMetricsDto>> getRedisMetrics() {
        log.debug("Getting Redis metrics");
        RedisMetricsDto metrics = queryBus.execute(new GetRedisMetricsQuery());
        return ResponseEntity.ok(ApiResponse.success("Redis metrics retrieved successfully", metrics));
    }

    /**
     * Get slow endpoints (endpoints with high latency)
     */
    @GetMapping("/slow-endpoints")
    @Operation(summary = "Get slow endpoints", description = "Returns list of slow API endpoints with performance metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SlowEndpointDto>>> getSlowEndpoints() {
        log.debug("Getting slow endpoints");
        List<SlowEndpointDto> endpoints = queryBus.execute(new GetSlowEndpointsQuery());
        return ResponseEntity.ok(ApiResponse.success("Slow endpoints retrieved successfully", endpoints));
    }
}
