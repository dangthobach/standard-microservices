package com.enterprise.business.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Virtual Thread Metrics Configuration
 * <p>
 * Exposes Prometheus metrics for monitoring virtual thread performance:
 * - jvm.threads.virtual.count: Number of live virtual threads (approximate)
 * - jvm.threads.virtual.peak: Peak number of virtual threads since startup
 * <p>
 * Note: Spring Boot 3.2+ with virtual threads enabled automatically exposes
 * some JVM thread metrics via Micrometer. This configuration adds additional
 * tracking for better observability.
 * <p>
 * Access metrics via:
 * - /actuator/metrics/jvm.threads.virtual.count
 * - /actuator/metrics/jvm.threads.virtual.peak
 * - /actuator/prometheus (for Prometheus scraping)
 * <p>
 * Example Prometheus queries:
 * - Current virtual threads: jvm_threads_virtual_count
 * - Peak virtual threads: jvm_threads_virtual_peak
 * - Rate of thread creation: rate(jvm_threads_virtual_count[5m])
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true")
public class VirtualThreadMetricsConfiguration {

    private final MeterRegistry meterRegistry;
    private final AtomicLong peakCount = new AtomicLong(0);
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void registerVirtualThreadMetrics() {
        // Virtual thread count (current) - approximate via Thread.activeCount()
        // Note: In Java 21+, Thread.activeCount() includes virtual threads
        Gauge.builder("jvm.threads.virtual.count", this, config -> {
            try {
                long count = Thread.activeCount();
                // Update peak if current count is higher
                long currentPeak = peakCount.get();
                if (count > currentPeak) {
                    peakCount.set(count);
                }
                return (double) count;
            } catch (Exception e) {
                log.warn("Failed to get virtual thread count: {}", e.getMessage());
                return 0.0;
            }
        })
        .description("Current number of live virtual threads (approximate)")
        .baseUnit("threads")
        .register(meterRegistry);

        // Track peak virtual thread count
        Gauge.builder("jvm.threads.virtual.peak", peakCount, AtomicLong::get)
            .description("Peak number of virtual threads since startup")
            .baseUnit("threads")
            .register(meterRegistry);

        // Start scheduler to periodically update peak (runs every 5 seconds)
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "virtual-thread-metrics-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                long currentCount = Thread.activeCount();
                long currentPeak = peakCount.get();
                if (currentCount > currentPeak) {
                    peakCount.set(currentCount);
                }
            } catch (Exception e) {
                log.warn("Failed to update peak virtual thread count: {}", e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);

        log.info("✅ Registered virtual thread metrics: jvm.threads.virtual.count, jvm.threads.virtual.peak");
    }
}

