package com.enterprise.gateway.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent Users (CCU) Metrics Configuration
 *
 * Exposes Prometheus metrics for monitoring concurrent active users:
 * - ccu_total: Total number of concurrent active users (real-time)
 * - ccu_by_service{service="..."}: CCU breakdown by downstream service (optional)
 *
 * Accuracy Mechanism (Heartbeat Pattern):
 * - JwtEnrichmentFilter updates "online:{userId}" key on every request
 * - Keys have short TTL (2-3 minutes) configured in application.yml
 * - CcuMetricsScheduler scans "online:*" keys every 30-60 seconds
 * - Result: Count only users who made a request within TTL window
 *
 * Why This Works:
 * - "online:*" keys expire after 2-3 min of inactivity
 * - Accurately reflects users currently interacting with the system
 * - NOT just "valid sessions" (which can persist 24h)
 *
 * Metrics are updated by CcuMetricsScheduler via Redis SCAN operation.
 * Prometheus scrapes these metrics at configured interval (typically 15-60s)
 * for time-series trending and alerting.
 *
 * Architecture:
 * ┌────────────────────────────┐
 * │ JwtEnrichmentFilter        │ (every request)
 * │  - SET online:{userId}     │
 * │  - TTL: 2-3 minutes        │
 * └─────────┬──────────────────┘
 *           ↓
 * ┌────────────────────────────┐
 * │ CcuMetricsScheduler        │ (every 30s)
 * │  - SCAN Redis online:*     │
 * │  - Count active users      │
 * │  - Update gauges           │
 * └─────────┬──────────────────┘
 *           ↓
 * ┌─────────────────────┐
 * │ AtomicLong Gauges   │ (this config)
 * │  - totalCcuGauge    │
 * │  - ccuByServiceMap  │
 * └─────────┬───────────┘
 *           ↓
 * ┌─────────────────────┐
 * │  MeterRegistry      │
 * │  ↓                  │
 * │  Prometheus         │
 * └─────────────────────┘
 *
 * Performance:
 * - Memory: ~5KB for gauges
 * - CPU: Negligible (just reads AtomicLong)
 * - Cardinality: Low (1 total + ~4-10 per-service)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CcuMetricsConfiguration {

    private final MeterRegistry meterRegistry;

    /**
     * Total CCU Gauge
     *
     * This AtomicLong is updated by CcuMetricsScheduler and read by
     * Micrometer Gauge for Prometheus scraping.
     *
     * Metric Name: ccu_total
     * Type: Gauge
     * Unit: users (active, not sessions)
     *
     * What This Counts:
     * - Unique users who made a request within the last 2-3 minutes
     * - NOT total valid sessions (which can persist 24h)
     * - Updated every 30 seconds by scanning "online:*" keys
     *
     * Example Prometheus queries:
     * - Current: ccu_total
     * - Peak 1h: max_over_time(ccu_total[1h])
     * - Average 24h: avg_over_time(ccu_total[24h])
     * - Growth rate: rate(ccu_total[5m])
     *
     * @return AtomicLong reference for scheduler to update
     */
    @Bean
    public AtomicLong totalCcuGauge() {
        AtomicLong totalCcu = new AtomicLong(0);

        Gauge.builder("ccu_total", totalCcu, AtomicLong::get)
            .description("Total concurrent active users (last 2-3 min activity)")
            .baseUnit("users")
            .register(meterRegistry);

        log.info("✅ Registered CCU total gauge metric: ccu_total (tracking active users)");
        return totalCcu;
    }

    /**
     * CCU by Service Gauges
     *
     * Optional enhancement to track CCU breakdown by downstream service.
     * Disabled by default for better performance (requires storing service info).
     *
     * Enable by setting ccu.metrics.tracking.by-service=true in application.yml
     *
     * Metric Name: ccu_by_service{service="iam-service"}
     * Type: Gauge
     * Unit: users (active, not sessions)
     * Tags: service (iam-service, business-service, etc.)
     *
     * Implementation Notes:
     * - Would require storing service info in online key value
     * - E.g., "online:{userId}" = "iam-service"
     * - Or separate keys: "online:{service}:{userId}"
     *
     * Example Prometheus queries:
     * - By service: sum by (service) (ccu_by_service)
     * - Top service: topk(3, ccu_by_service)
     *
     * @return Map of service name to AtomicLong counter
     */
    @Bean
    public Map<String, AtomicLong> ccuByServiceGauges() {
        Map<String, AtomicLong> ccuByService = new ConcurrentHashMap<>();

        // Pre-register gauges for known downstream services
        List<String> services = List.of(
            "iam-service",
            "business-service",
            "process-service",
            "integration-service"
        );

        for (String service : services) {
            AtomicLong counter = new AtomicLong(0);
            ccuByService.put(service, counter);

            Gauge.builder("ccu_by_service", counter, AtomicLong::get)
                .description("Concurrent active users by downstream service")
                .tag("service", service)
                .baseUnit("users")
                .register(meterRegistry);

            log.info("✅ Registered CCU gauge for service: {}", service);
        }

        log.info("✅ Registered {} service-level CCU gauges", services.size());
        return ccuByService;
    }

    /**
     * CCU by Endpoint Gauges (Optional - Future Enhancement)
     *
     * For finer granularity tracking of which endpoints users are accessing.
     * Not implemented in Phase 1 to minimize overhead.
     *
     * Implementation notes:
     * - Would require tracking lastAccessedEndpoint in UserSession
     * - Higher cardinality (hundreds of endpoints)
     * - Consider sampling or top-K endpoints only
     *
     * @return Map of endpoint to AtomicLong counter
     */
    @Bean
    public Map<String, AtomicLong> ccuByEndpointGauges() {
        Map<String, AtomicLong> ccuByEndpoint = new ConcurrentHashMap<>();
        log.info("✅ Initialized CCU by endpoint tracking (disabled by default)");
        return ccuByEndpoint;
    }
}
