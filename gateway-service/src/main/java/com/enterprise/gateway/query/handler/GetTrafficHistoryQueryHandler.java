package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.TrafficDataDto;
import com.enterprise.gateway.query.GetTrafficHistoryQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for getting traffic history
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetTrafficHistoryQueryHandler implements QueryHandler<GetTrafficHistoryQuery, List<TrafficDataDto>> {

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<TrafficDataDto> handle(GetTrafficHistoryQuery query) {
        List<TrafficDataDto> trafficData = new ArrayList<>();

        try {
            // Get last 24 hours of data in 5-minute intervals
            Instant now = Instant.now();
            int intervals = 288; // 24 hours * 12 (5-minute intervals per hour)
            List<String> keys = new ArrayList<>();
            List<Instant> timestamps = new ArrayList<>();

            // 1. Generate all keys
            for (int i = intervals - 1; i >= 0; i--) {
                Instant timestamp = now.minus(i * 5L, ChronoUnit.MINUTES);
                long bucket = timestamp.getEpochSecond() / 300;
                String bucketTimestamp = Instant.ofEpochSecond(bucket * 300).toString();

                keys.add("dashboard:traffic:history:" + bucketTimestamp + ":requests");
                keys.add("dashboard:traffic:history:" + bucketTimestamp + ":errors");
                timestamps.add(Instant.ofEpochSecond(bucket * 300));
            }

            // 2. Fetch all values in one round-trip (Pipeline)
            List<String> values = redisTemplate.opsForValue().multiGet(keys);

            // 3. Process results
            if (values != null && values.size() >= timestamps.size() * 2) {
                for (int i = 0; i < timestamps.size(); i++) {
                    String requestStr = values.get(i * 2);
                    String errorStr = values.get(i * 2 + 1);

                    long requests = requestStr != null ? Long.parseLong(requestStr) : 0L;
                    long errors = errorStr != null ? Long.parseLong(errorStr) : 0L;

                    if (requests > 0 || errors > 0) {
                        trafficData.add(TrafficDataDto.builder()
                                .timestamp(timestamps.get(i))
                                .requests(requests)
                                .errors(errors)
                                .build());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to get traffic history: {}", e.getMessage(), e);
        }

        return trafficData;
    }
}
