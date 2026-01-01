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
            // 1. SCAN for keys (safer than KEYS command in production)
            List<String> avgKeys = new ArrayList<>();
            redisTemplate.execute(connection -> {
                try (var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("dashboard:slow:endpoint:*:avg")
                        .count(100)
                        .build())) {
                    while (cursor.hasNext()) {
                        avgKeys.add(new String(cursor.next()));
                    }
                }
                return null;
            }, true);

            if (!avgKeys.isEmpty()) {
                // 2. Prepare all keys for multiGet (Pipeline)
                List<String> allKeys = new ArrayList<>();
                List<EndpointInfo> endpointInfos = new ArrayList<>();

                for (String avgKey : avgKeys) {
                    try {
                        // Extract method and path from key
                        // Key format: dashboard:slow:endpoint:METHOD:path:avg
                        String keyWithoutPrefix = avgKey.replace("dashboard:slow:endpoint:", "");
                        String keyWithoutSuffix = keyWithoutPrefix.substring(0, keyWithoutPrefix.lastIndexOf(":avg"));

                        int firstColon = keyWithoutSuffix.indexOf(":");
                        String method = keyWithoutSuffix.substring(0, firstColon);
                        String path = keyWithoutSuffix.substring(firstColon + 1);

                        String baseKey = "dashboard:slow:endpoint:" + method + ":" + path;

                        // Add keys in order: avg, p95, calls
                        allKeys.add(baseKey + ":avg");
                        allKeys.add(baseKey + ":p95");
                        allKeys.add(baseKey + ":calls");

                        endpointInfos.add(new EndpointInfo(method, path));
                    } catch (Exception e) {
                        log.error("Failed to parse slow endpoint key {}: {}", avgKey, e.getMessage());
                    }
                }

                // 3. Fetch all values in one round-trip
                List<String> values = redisTemplate.opsForValue().multiGet(allKeys);

                // 4. Process results
                if (values != null && values.size() >= endpointInfos.size() * 3) {
                    for (int i = 0; i < endpointInfos.size(); i++) {
                        try {
                            EndpointInfo info = endpointInfos.get(i);
                            String avgLatencyStr = values.get(i * 3);
                            String p95LatencyStr = values.get(i * 3 + 1);
                            String callsStr = values.get(i * 3 + 2);

                            Long avgLatency = avgLatencyStr != null ? (long) Double.parseDouble(avgLatencyStr) : 0L;
                            Long p95Latency = p95LatencyStr != null ? Long.parseLong(p95LatencyStr) : 0L;
                            Long calls = callsStr != null ? Long.parseLong(callsStr) : 0L;

                            slowEndpoints.add(SlowEndpointDto.builder()
                                .method(info.method)
                                .path(info.path)
                                .avgLatency(avgLatency)
                                .p95Latency(p95Latency)
                                .calls(calls)
                                .build());
                        } catch (Exception e) {
                            log.error("Failed to parse slow endpoint values at index {}: {}", i, e.getMessage());
                        }
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

    /**
     * Helper class to store endpoint method and path during processing
     */
    private static class EndpointInfo {
        final String method;
        final String path;

        EndpointInfo(String method, String path) {
            this.method = method;
            this.path = path;
        }
    }
}
