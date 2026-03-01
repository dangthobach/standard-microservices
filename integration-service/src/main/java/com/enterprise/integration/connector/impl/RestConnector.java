package com.enterprise.integration.connector.impl;

import com.enterprise.integration.connector.AuthorizationType;
import com.enterprise.integration.connector.Connector;
import com.enterprise.integration.connector.ConnectorType;
import com.enterprise.integration.dto.ConnectorRequest;
import com.enterprise.integration.dto.ConnectorResponse;
import com.enterprise.integration.entity.ConnectorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestConnector implements Connector {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Override
    public ConnectorResponse execute(ConnectorConfig config, ConnectorRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // Parse auth config
            JsonNode authNode = config.getAuthConfig() != null ? objectMapper.readTree(config.getAuthConfig()) : null;

            // Build Request
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(config.getEndpointUrl())
                    .contentType(MediaType.APPLICATION_JSON);

            // Add Headers
            if (config.getDefaultHeaders() != null) {
                try {
                    Map<String, String> defaultHeaders = objectMapper.readValue(
                            config.getDefaultHeaders(), Map.class);
                    defaultHeaders.forEach(requestSpec::header);
                } catch (Exception e) {
                    log.error("Failed to parse default headers for connector {}", config.getName(), e);
                }
            }

            if (request.getHeaders() != null) {
                request.getHeaders().forEach(requestSpec::header);
            }

            // Handle Authorization
            applyAuthorization(requestSpec, config.getAuthType(), authNode);

            // Execute
            String requestBody = objectMapper.writeValueAsString(request.getPayload());

            return requestSpec.body(requestBody)
                    .exchange((req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        return ConnectorResponse.builder()
                                .success(res.getStatusCode().is2xxSuccessful())
                                .statusCode(res.getStatusCode().value())
                                .responseBody(body)
                                .durationMs(System.currentTimeMillis() - startTime)
                                .build();
                    });

        } catch (Exception e) {
            log.error("Connector execution failed for {}", config.getName(), e);
            return ConnectorResponse.builder()
                    .success(false)
                    .statusCode(500)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private void applyAuthorization(RestClient.RequestBodySpec requestSpec,
            AuthorizationType authType,
            JsonNode authNode) {
        if (authType == AuthorizationType.NONE || authNode == null)
            return;

        switch (authType) {
            case BASIC_AUTH -> {
                if (authNode.has("username") && authNode.has("password")) {
                    String username = authNode.get("username").asText();
                    String password = authNode.get("password").asText();
                    String token = java.util.Base64.getEncoder()
                            .encodeToString((username + ":" + password).getBytes());
                    requestSpec.header("Authorization", "Basic " + token);
                }
            }
            case BEARER_TOKEN -> {
                if (authNode.has("token")) {
                    requestSpec.header("Authorization", "Bearer " + authNode.get("token").asText());
                }
            }
            case API_KEY -> {
                if (authNode.has("keyName") && authNode.has("keyValue") && authNode.has("in")) {
                    String keyName = authNode.get("keyName").asText();
                    String keyValue = authNode.get("keyValue").asText();
                    String in = authNode.get("in").asText();

                    if ("header".equalsIgnoreCase(in)) {
                        requestSpec.header(keyName, keyValue);
                    }
                    // "query" param handling would require URI builder changes, simplified for now
                }
            }
            default -> log.warn("Unsupported auth type: {}", authType);
        }
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.REST;
    }
}
