package com.enterprise.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorResponse {
    private boolean success;
    private int statusCode;
    private String responseBody;
    private Map<String, String> headers;
    private String errorMessage;
    private long durationMs;
}
