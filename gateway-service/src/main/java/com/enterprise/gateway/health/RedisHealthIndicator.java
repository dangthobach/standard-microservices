package com.enterprise.gateway.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis Health Indicator
 *
 * Checks:
 * - Connection availability
 * - Response time
 * - Memory usage
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Override
    public Mono<Health> health() {
        long startTime = System.currentTimeMillis();

        return redisTemplate.opsForValue()
                .set("health:check", "ping", Duration.ofSeconds(5))
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        long responseTime = System.currentTimeMillis() - startTime;

                        return redisTemplate.execute(connection ->
                                connection.serverCommands().info("memory")
                        ).collectList().map(memoryInfo -> {
                            Health.Builder builder = Health.up()
                                    .withDetail("responseTime", responseTime + "ms")
                                    .withDetail("status", "Connected");

                            if (!memoryInfo.isEmpty()) {
                                String info = memoryInfo.get(0).toString();
                                builder.withDetail("memory", parseMemoryUsage(info));
                            }

                            return builder.build();
                        });
                    } else {
                        return Mono.just(Health.down()
                                .withDetail("error", "Failed to ping Redis")
                                .build());
                    }
                })
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(error -> {
                    log.error("Redis health check failed", error);
                    return Mono.just(Health.down()
                            .withException(error)
                            .build());
                });
    }

    private String parseMemoryUsage(String info) {
        // Parse used_memory from Redis INFO
        for (String line : info.split("\n")) {
            if (line.startsWith("used_memory_human:")) {
                return line.substring("used_memory_human:".length()).trim();
            }
        }
        return "unknown";
    }
}
