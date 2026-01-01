package com.enterprise.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Traffic data DTO for dashboard charts
 * Contains time-series traffic metrics
 */
@Data
@Builder
public class TrafficDataDto {

    /**
     * Timestamp of the data point
     */
    private Instant timestamp;

    /**
     * Number of requests in this time period
     */
    private Long requests;

    /**
     * Number of errors in this time period
     */
    private Long errors;
}
