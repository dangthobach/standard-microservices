package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.DatabaseMetricsDto;

import java.util.List;

/**
 * Query to get database metrics
 */
public record GetDatabaseMetricsQuery() implements Query<List<DatabaseMetricsDto>> {
}
