package com.enterprise.gateway.query.handler;

import com.enterprise.common.cqrs.QueryHandler;
import com.enterprise.gateway.dto.RedisMetricsDto;
import com.enterprise.gateway.query.GetRedisMetricsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Handler for getting Redis metrics
 * Uses Redis INFO command to get server statistics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetRedisMetricsQueryHandler implements QueryHandler<GetRedisMetricsQuery, RedisMetricsDto> {

    private final StringRedisTemplate redisTemplate;

    @Override
    public RedisMetricsDto handle(GetRedisMetricsQuery query) {
        try {
            Properties info = redisTemplate.execute((RedisConnection connection) -> {
                return connection.serverCommands().info();
            });

            if (info != null) {
                return parseRedisInfo(info);
            }

        } catch (Exception e) {
            log.error("Failed to get Redis metrics: {}", e.getMessage(), e);
        }

        // Return default metrics on error
        return RedisMetricsDto.builder()
            .connections(0)
            .memoryUsed(0.0)
            .memoryTotal(4.0)
            .hitRate(0.0)
            .evictions(0L)
            .opsPerSec(0L)
            .build();
    }

    private RedisMetricsDto parseRedisInfo(Properties info) {
        try {
            // Parse memory metrics
            String usedMemoryStr = info.getProperty("used_memory", "0");
            String maxMemoryStr = info.getProperty("maxmemory", "0");
            long usedMemoryBytes = Long.parseLong(usedMemoryStr);
            long maxMemoryBytes = Long.parseLong(maxMemoryStr);

            double usedMemoryGB = usedMemoryBytes / (1024.0 * 1024.0 * 1024.0);
            double maxMemoryGB = maxMemoryBytes > 0 ? maxMemoryBytes / (1024.0 * 1024.0 * 1024.0) : 4.0;

            // Parse connection metrics
            int connections = Integer.parseInt(info.getProperty("connected_clients", "0"));

            // Parse stats metrics
            long keyspaceHits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long keyspaceMisses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            double hitRate = 0.0;
            if (keyspaceHits + keyspaceMisses > 0) {
                hitRate = (keyspaceHits * 100.0) / (keyspaceHits + keyspaceMisses);
            }

            // Parse eviction metrics
            long evictions = Long.parseLong(info.getProperty("evicted_keys", "0"));

            // Parse operations per second
            long opsPerSec = Long.parseLong(info.getProperty("instantaneous_ops_per_sec", "0"));

            return RedisMetricsDto.builder()
                .connections(connections)
                .memoryUsed(usedMemoryGB)
                .memoryTotal(maxMemoryGB)
                .hitRate(hitRate)
                .evictions(evictions)
                .opsPerSec(opsPerSec)
                .build();

        } catch (Exception e) {
            log.error("Failed to parse Redis info: {}", e.getMessage(), e);
            return RedisMetricsDto.builder()
                .connections(0)
                .memoryUsed(0.0)
                .memoryTotal(4.0)
                .hitRate(0.0)
                .evictions(0L)
                .opsPerSec(0L)
                .build();
        }
    }
}
