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
public class ConnectorRequest {
    private String connectorName;
    private Map<String, Object> payload;
    private Map<String, String> headers;
}
