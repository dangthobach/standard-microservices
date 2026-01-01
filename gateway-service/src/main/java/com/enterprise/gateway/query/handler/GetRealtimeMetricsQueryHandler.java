package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.RealtimeMetricsDto;
import com.enterprise.gateway.query.GetRealtimeMetricsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

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
            // Get CCU from online users
            Set<String> onlineKeys = redisTemplate.keys("online:*");
            Long ccu = onlineKeys != null ? (long) onlineKeys.size() : 0L;

            // Get RPS from counter
            String rpsStr = redisTemplate.opsForValue().get("dashboard:rps");
            Long rps = rpsStr != null ? Long.parseLong(rpsStr) : 0L;

            // Get average latency
            String latencyStr = redisTemplate.opsForValue().get("dashboard:latency:avg");
            Double avgLatency = latencyStr != null ? Double.parseDouble(latencyStr) : 0.0;

            // Calculate error rate
            String errorCountStr = redisTemplate.opsForValue().get("dashboard:error:count");
            String requestCountStr = redisTemplate.opsForValue().get("dashboard:request:count");

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
