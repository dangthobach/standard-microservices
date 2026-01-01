package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.SlowEndpointDto;

import java.util.List;

/**
 * Query to get slow endpoints
 */
public record GetSlowEndpointsQuery() implements Query<List<SlowEndpointDto>> {
}
