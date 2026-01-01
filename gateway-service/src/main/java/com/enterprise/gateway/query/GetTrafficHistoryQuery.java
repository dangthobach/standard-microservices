package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.TrafficDataDto;

import java.util.List;

/**
 * Query to get traffic history for charts
 */
public record GetTrafficHistoryQuery() implements Query<List<TrafficDataDto>> {
}
