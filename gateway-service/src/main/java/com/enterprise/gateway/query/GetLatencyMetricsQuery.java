package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.LatencyDataDto;

import java.util.List;

/**
 * Query to get latency percentiles by service
 */
public record GetLatencyMetricsQuery() implements Query<List<LatencyDataDto>> {
}
