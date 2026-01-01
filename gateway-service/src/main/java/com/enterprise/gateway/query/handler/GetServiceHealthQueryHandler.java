package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.ServiceHealthDto;
import com.enterprise.gateway.query.GetServiceHealthQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for getting service health status
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetServiceHealthQueryHandler implements QueryHandler<GetServiceHealthQuery, List<ServiceHealthDto>> {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<ServiceHealthDto> handle(GetServiceHealthQuery query) {
        List<ServiceHealthDto> healthList = new ArrayList<>();

        try {
            // 1. Scan for keys (safer than KEYS command)
            List<String> keys = new ArrayList<>();
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection
                        .scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match("dashboard:service:*:health")
                                .count(100)
                                .build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next()));
                    }
                }
                return null;
            });

            // 2. Fetch all values in one round-trip (MultiGet)
            if (!keys.isEmpty()) {
                List<String> values = redisTemplate.opsForValue().multiGet(keys);

                if (values != null) {
                    for (String json : values) {
                        try {
                            if (json != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> healthData = objectMapper.readValue(json, Map.class);

                                ServiceHealthDto dto = ServiceHealthDto.builder()
                                        .name((String) healthData.get("name"))
                                        .status((String) healthData.get("status"))
                                        .cpu(getDoubleValue(healthData, "cpu"))
                                        .memory(getDoubleValue(healthData, "memory"))
                                        .uptime((String) healthData.get("uptime"))
                                        .requests(getLongValue(healthData, "requests"))
                                        .errors(getLongValue(healthData, "errors"))
                                        .build();

                                healthList.add(dto);
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse health data: {}", e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to get service health: {}", e.getMessage(), e);
        }

        return healthList;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
