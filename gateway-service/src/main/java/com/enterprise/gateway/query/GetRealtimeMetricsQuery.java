package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.RealtimeMetricsDto;

/**
 * Query to get real-time dashboard metrics
 */
public record GetRealtimeMetricsQuery() implements Query<RealtimeMetricsDto> {
}
