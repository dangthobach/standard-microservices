package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.SlowEndpointDto;
import com.enterprise.gateway.query.GetSlowEndpointsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handler for getting slow endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetSlowEndpointsQueryHandler implements QueryHandler<GetSlowEndpointsQuery, List<SlowEndpointDto>> {

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<SlowEndpointDto> handle(GetSlowEndpointsQuery query) {
        List<SlowEndpointDto> slowEndpoints = new ArrayList<>();

        try {
            Set<String> keys = redisTemplate.keys("dashboard:slow:endpoint:*:avg");

            if (keys != null) {
                for (String avgKey : keys) {
                    try {
                        // Extract method and path from key
                        // Key format: dashboard:slow:endpoint:METHOD:path:avg
                        String keyWithoutPrefix = avgKey.replace("dashboard:slow:endpoint:", "");
                        String keyWithoutSuffix = keyWithoutPrefix.substring(0, keyWithoutPrefix.lastIndexOf(":avg"));

                        int firstColon = keyWithoutSuffix.indexOf(":");
                        String method = keyWithoutSuffix.substring(0, firstColon);
                        String path = keyWithoutSuffix.substring(firstColon + 1);

                        String baseKey = "dashboard:slow:endpoint:" + method + ":" + path;

                        String avgLatencyStr = redisTemplate.opsForValue().get(baseKey + ":avg");
                        String p95LatencyStr = redisTemplate.opsForValue().get(baseKey + ":p95");
                        String callsStr = redisTemplate.opsForValue().get(baseKey + ":calls");

                        Long avgLatency = avgLatencyStr != null ? (long) Double.parseDouble(avgLatencyStr) : 0L;
                        Long p95Latency = p95LatencyStr != null ? Long.parseLong(p95LatencyStr) : 0L;
                        Long calls = callsStr != null ? Long.parseLong(callsStr) : 0L;

                        slowEndpoints.add(SlowEndpointDto.builder()
                            .method(method)
                            .path(path)
                            .avgLatency(avgLatency)
                            .p95Latency(p95Latency)
                            .calls(calls)
                            .build());

                    } catch (Exception e) {
                        log.error("Failed to parse slow endpoint from key {}: {}", avgKey, e.getMessage());
                    }
                }
            }

            // Sort by average latency descending
            slowEndpoints.sort((a, b) -> Long.compare(b.getAvgLatency(), a.getAvgLatency()));

        } catch (Exception e) {
            log.error("Failed to get slow endpoints: {}", e.getMessage(), e);
        }

        return slowEndpoints;
    }
}
