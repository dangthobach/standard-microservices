package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.LatencyDataDto;
import com.enterprise.gateway.query.GetLatencyMetricsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handler for getting latency metrics by service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetLatencyMetricsQueryHandler implements QueryHandler<GetLatencyMetricsQuery, List<LatencyDataDto>> {

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<LatencyDataDto> handle(GetLatencyMetricsQuery query) {
        List<LatencyDataDto> latencyData = new ArrayList<>();

        try {
            Set<String> keys = redisTemplate.keys("dashboard:service:*:latency");

            if (keys != null) {
                for (String key : keys) {
                    try {
                        // Extract service name from key
                        // Key format: dashboard:service:service-name:latency
                        String serviceName = extractServiceName(key);

                        String avgLatencyStr = redisTemplate.opsForValue().get(key);
                        Long avgLatency = avgLatencyStr != null ? (long) Double.parseDouble(avgLatencyStr) : 0L;

                        // For now, use approximations for P95 and P99
                        // In production, you'd use a proper percentile tracking library
                        Long p95 = (long) (avgLatency * 1.5);
                        Long p99 = (long) (avgLatency * 2.0);

                        latencyData.add(LatencyDataDto.builder()
                            .service(serviceName)
                            .p50(avgLatency)
                            .p95(p95)
                            .p99(p99)
                            .build());

                    } catch (Exception e) {
                        log.error("Failed to parse latency data from key {}: {}", key, e.getMessage());
                    }
                }
            }

            // Add Gateway latency from global metrics
            String gatewayLatencyStr = redisTemplate.opsForValue().get("dashboard:latency:avg");
            if (gatewayLatencyStr != null) {
                Long gatewayLatency = (long) Double.parseDouble(gatewayLatencyStr);
                latencyData.add(LatencyDataDto.builder()
                    .service("Gateway")
                    .p50(gatewayLatency)
                    .p95((long) (gatewayLatency * 1.5))
                    .p99((long) (gatewayLatency * 2.0))
                    .build());
            }

        } catch (Exception e) {
            log.error("Failed to get latency metrics: {}", e.getMessage(), e);
        }

        return latencyData;
    }

    private String extractServiceName(String key) {
        // Key format: dashboard:service:service-name:latency
        String[] parts = key.split(":");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "Unknown";
    }
}
