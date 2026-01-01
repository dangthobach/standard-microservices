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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Dashboard API Controller (Non-Blocking WebFlux)
 *
 * CRITICAL: This controller runs in WebFlux (Netty event loop).
 * All QueryBus calls are BLOCKING Redis operations, which would block the event loop.
 *
 * Solution: Wrap all blocking calls in Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())
 * to execute on separate thread pool, keeping Netty event loop non-blocking.
 *
 * Performance:
 * - Without subscribeOn: Blocks Netty → Gateway hangs under load
 * - With subscribeOn: Non-blocking → Gateway handles 10K+ req/sec
 *
 * Authorization:
 * - Uses dynamic role-based authorization via @dashboardSecurity.hasAccess()
 * - Configurable via application.yml: dashboard.security.allowed-roles
 * - Default: Only ADMIN role has access
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
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/realtime")
    @Operation(summary = "Get real-time metrics", description = "Returns current CCU, RPS, error rate, and average latency")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<RealtimeMetricsDto>>> getRealtimeMetrics() {
        log.debug("Getting real-time metrics");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetRealtimeMetricsQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(metrics -> ResponseEntity.ok(
                ApiResponse.success("Real-time metrics retrieved successfully", metrics)
            ))
            .doOnError(error -> log.error("Failed to get real-time metrics", error));
    }

    /**
     * Get service health status
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/services")
    @Operation(summary = "Get service health", description = "Returns health status of all microservices")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<ServiceHealthDto>>>> getServiceHealth() {
        log.debug("Getting service health");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetServiceHealthQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(services -> ResponseEntity.ok(
                ApiResponse.success("Service health retrieved successfully", services)
            ))
            .doOnError(error -> log.error("Failed to get service health", error));
    }

    /**
     * Get traffic history for charts (last 24 hours)
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/traffic")
    @Operation(summary = "Get traffic history", description = "Returns traffic data for the last 24 hours in 5-minute intervals")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<TrafficDataDto>>>> getTrafficHistory() {
        log.debug("Getting traffic history");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetTrafficHistoryQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(traffic -> ResponseEntity.ok(
                ApiResponse.success("Traffic history retrieved successfully", traffic)
            ))
            .doOnError(error -> log.error("Failed to get traffic history", error));
    }

    /**
     * Get database metrics from all microservices (connections, queries, cache hit rate)
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/database")
    @Operation(summary = "Get database metrics", description = "Returns database connection pool and query statistics from all microservices")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<DatabaseMetricsDto>>>> getDatabaseMetrics() {
        log.debug("Getting database metrics");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetDatabaseMetricsQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(metrics -> ResponseEntity.ok(
                ApiResponse.success("Database metrics retrieved successfully", metrics)
            ))
            .doOnError(error -> log.error("Failed to get database metrics", error));
    }

    /**
     * Get latency percentiles by service
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/latency")
    @Operation(summary = "Get latency heatmap", description = "Returns P50, P95, P99 latency by service")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<LatencyDataDto>>>> getLatencyMetrics() {
        log.debug("Getting latency metrics");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetLatencyMetricsQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(latency -> ResponseEntity.ok(
                ApiResponse.success("Latency metrics retrieved successfully", latency)
            ))
            .doOnError(error -> log.error("Failed to get latency metrics", error));
    }

    /**
     * Get Redis metrics (memory, connections, hit rate)
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/redis")
    @Operation(summary = "Get Redis metrics", description = "Returns Redis memory usage, connections, and performance metrics")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<RedisMetricsDto>>> getRedisMetrics() {
        log.debug("Getting Redis metrics");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetRedisMetricsQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(metrics -> ResponseEntity.ok(
                ApiResponse.success("Redis metrics retrieved successfully", metrics)
            ))
            .doOnError(error -> log.error("Failed to get Redis metrics", error));
    }

    /**
     * Get slow endpoints (endpoints with high latency)
     *
     * NON-BLOCKING: Executes QueryBus on boundedElastic thread pool
     */
    @GetMapping("/slow-endpoints")
    @Operation(summary = "Get slow endpoints", description = "Returns list of slow API endpoints with performance metrics")
    @PreAuthorize("@dashboardSecurity.hasAccess(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<SlowEndpointDto>>>> getSlowEndpoints() {
        log.debug("Getting slow endpoints");
        return Mono.fromCallable(() -> queryBus.dispatch(new GetSlowEndpointsQuery()))
            .subscribeOn(Schedulers.boundedElastic())
            .map(endpoints -> ResponseEntity.ok(
                ApiResponse.success("Slow endpoints retrieved successfully", endpoints)
            ))
            .doOnError(error -> log.error("Failed to get slow endpoints", error));
    }
}
