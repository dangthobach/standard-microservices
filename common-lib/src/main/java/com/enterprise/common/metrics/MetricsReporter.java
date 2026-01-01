package com.enterprise.common.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Automatic metrics reporter for microservices
 * Reports CPU, memory, and request metrics to Redis for dashboard consumption
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "metrics.reporter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsReporter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${metrics.reporter.heartbeat-interval:5000}")
    private long heartbeatInterval;

    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    /**
     * Record a request with its latency
     * @param latencyMs Request latency in milliseconds
     */
    public void recordRequest(long latencyMs) {
        requestCount.incrementAndGet();

        // Update latency metrics in Redis (Exponential Moving Average)
        try {
            String key = "dashboard:service:" + serviceName + ":latency";
            updateExponentialMovingAverage(key, (double) latencyMs, 0.2); // Alpha = 0.2
        } catch (Exception e) {
            log.warn("Failed to record request latency: {}", e.getMessage());
        }
    }

    /**
     * Increment error counter
     */
    public void incrementError() {
        errorCount.incrementAndGet();
    }

    /**
     * Report service health metrics to Redis every 5 seconds
     */
    @Scheduled(fixedDelayString = "${metrics.reporter.heartbeat-interval:5000}")
    public void reportHealth() {
        try {
            String key = "dashboard:service:" + serviceName + ":health";

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("name", serviceName);
            healthData.put("cpu", getCpuUsage());
            healthData.put("memory", getMemoryUsage());
            healthData.put("uptime", getUptime());
            healthData.put("requests", requestCount.get());
            healthData.put("errors", errorCount.get());
            healthData.put("status", determineStatus(getCpuUsage(), getMemoryUsage()));
            healthData.put("lastHeartbeat", Instant.now().toString());

            String json = objectMapper.writeValueAsString(healthData);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(30)); // 30s TTL

            log.debug("Reported health metrics for {}: CPU={}%, Memory={}%",
                serviceName, healthData.get("cpu"), healthData.get("memory"));
        } catch (Exception e) {
            log.error("Failed to report health metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Get CPU usage percentage
     */
    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double cpuLoad = sunOsBean.getProcessCpuLoad();
                return cpuLoad >= 0 ? cpuLoad * 100.0 : 0.0;
            }
            double systemLoad = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            return systemLoad >= 0 ? (systemLoad / processors) * 100.0 : 0.0;
        } catch (Exception e) {
            log.warn("Failed to get CPU usage: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get memory usage percentage
     */
    private double getMemoryUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            long max = memoryBean.getHeapMemoryUsage().getMax();
            return max > 0 ? (used * 100.0) / max : 0.0;
        } catch (Exception e) {
            log.warn("Failed to get memory usage: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get uptime in human-readable format
     */
    private String getUptime() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long days = uptime.toDays();
        long hours = uptime.toHours() % 24;
        long minutes = uptime.toMinutes() % 60;

        if (days > 0) {
            return String.format("%dd %dh", days, hours);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * Determine service status based on CPU and memory usage
     */
    private String determineStatus(double cpu, double memory) {
        if (cpu > 80 || memory > 80) {
            return "critical";
        } else if (cpu > 60 || memory > 60) {
            return "warning";
        }
        return "healthy";
    }

    /**
     * Update exponential moving average in Redis
     */
    private void updateExponentialMovingAverage(String key, double newValue, double alpha) {
        try {
            String currentValueStr = redisTemplate.opsForValue().get(key);
            double currentValue = currentValueStr != null ? Double.parseDouble(currentValueStr) : newValue;
            double ema = alpha * newValue + (1 - alpha) * currentValue;
            redisTemplate.opsForValue().set(key, String.valueOf(ema), Duration.ofMinutes(5));
        } catch (Exception e) {
            // Initialize with new value if error
            redisTemplate.opsForValue().set(key, String.valueOf(newValue), Duration.ofMinutes(5));
        }
    }
}
