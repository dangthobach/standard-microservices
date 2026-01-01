package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.RealtimeMetricsDto;
import com.enterprise.gateway.query.GetRealtimeMetricsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler for getting real-time metrics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetRealtimeMetricsQueryHandler implements QueryHandler<GetRealtimeMetricsQuery, RealtimeMetricsDto> {

    private final StringRedisTemplate redisTemplate;

    @Override
    public RealtimeMetricsDto handle(GetRealtimeMetricsQuery query) {
        try {
            // Get CCU from online users using SCAN (safer than KEYS in production)
            final long[] ccuCount = {0};
            redisTemplate.execute(connection -> {
                try (var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("online:*")
                        .count(1000)
                        .build())) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        ccuCount[0]++;
                    }
                }
                return null;
            }, true);
            Long ccu = ccuCount[0];

            // Fetch all other metrics in one round-trip (Pipeline)
            List<String> keys = List.of(
                "dashboard:rps",
                "dashboard:latency:avg",
                "dashboard:error:count",
                "dashboard:request:count"
            );
            List<String> values = redisTemplate.opsForValue().multiGet(keys);

            String rpsStr = values != null && values.size() > 0 ? values.get(0) : null;
            String latencyStr = values != null && values.size() > 1 ? values.get(1) : null;
            String errorCountStr = values != null && values.size() > 2 ? values.get(2) : null;
            String requestCountStr = values != null && values.size() > 3 ? values.get(3) : null;

            Long rps = rpsStr != null ? Long.parseLong(rpsStr) : 0L;
            Double avgLatency = latencyStr != null ? Double.parseDouble(latencyStr) : 0.0;
            Long errorCount = errorCountStr != null ? Long.parseLong(errorCountStr) : 0L;
            Long requestCount = requestCountStr != null ? Long.parseLong(requestCountStr) : 1L;

            Double errorRate = requestCount > 0 ? (errorCount.doubleValue() / requestCount.doubleValue()) : 0.0;

            return RealtimeMetricsDto.builder()
                .ccu(ccu)
                .rps(rps)
                .errorRate(errorRate)
                .avgLatency(avgLatency)
                .build();

        } catch (Exception e) {
            log.error("Failed to get realtime metrics: {}", e.getMessage(), e);
            return RealtimeMetricsDto.builder()
                .ccu(0L)
                .rps(0L)
                .errorRate(0.0)
                .avgLatency(0.0)
                .build();
        }
    }
}
