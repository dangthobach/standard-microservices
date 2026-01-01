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
            // 1. SCAN for service latency keys
            List<String> keys = new ArrayList<>();
            redisTemplate.execute(connection -> {
                try (var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("dashboard:service:*:latency")
                        .count(100)
                        .build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next()));
                    }
                }
                return null;
            }, true);

            // 2. Add gateway latency key
            keys.add("dashboard:latency:avg");

            // 3. Fetch all values in one round-trip (Pipeline)
            if (!keys.isEmpty()) {
                List<String> values = redisTemplate.opsForValue().multiGet(keys);

                if (values != null) {
                    for (int i = 0; i < keys.size(); i++) {
                        try {
                            String key = keys.get(i);
                            String avgLatencyStr = values.get(i);

                            if (avgLatencyStr != null) {
                                Long avgLatency = (long) Double.parseDouble(avgLatencyStr);

                                // For now, use approximations for P95 and P99
                                // In production, you'd use a proper percentile tracking library
                                Long p95 = (long) (avgLatency * 1.5);
                                Long p99 = (long) (avgLatency * 2.0);

                                String serviceName = key.equals("dashboard:latency:avg")
                                    ? "Gateway"
                                    : extractServiceName(key);

                                latencyData.add(LatencyDataDto.builder()
                                    .service(serviceName)
                                    .p50(avgLatency)
                                    .p95(p95)
                                    .p99(p99)
                                    .build());
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse latency data from key {}: {}", keys.get(i), e.getMessage());
                        }
                    }
                }
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
