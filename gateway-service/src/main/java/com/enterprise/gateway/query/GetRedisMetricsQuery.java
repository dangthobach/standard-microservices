package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.RedisMetricsDto;

/**
 * Query to get Redis metrics
 */
public record GetRedisMetricsQuery() implements Query<RedisMetricsDto> {
}
