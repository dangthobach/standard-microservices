package com.enterprise.gateway.query;

import com.enterprise.common.cqrs.Query;
import com.enterprise.gateway.dto.ServiceHealthDto;

import java.util.List;

/**
 * Query to get service health status
 */
public record GetServiceHealthQuery() implements Query<List<ServiceHealthDto>> {
}
