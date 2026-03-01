package com.enterprise.integration.service;

import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.integration.connector.Connector;
import com.enterprise.integration.connector.ConnectorFactory;
import com.enterprise.integration.dto.ConnectorRequest;
import com.enterprise.integration.dto.ConnectorResponse;
import com.enterprise.integration.entity.ConnectorConfig;
import com.enterprise.integration.entity.ConnectorExecution;
import com.enterprise.integration.repository.ConnectorConfigRepository;
import com.enterprise.integration.repository.ConnectorExecutionRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorService {

    private final ConnectorConfigRepository configRepository;
    private final ConnectorExecutionRepository executionRepository;
    private final ConnectorFactory connectorFactory;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Execute a connector by name with payload.
     */
    public ConnectorResponse executeConnector(String connectorName, ConnectorRequest request) {
        ConnectorConfig config = configRepository.findByName(connectorName)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectorConfig", "name", connectorName));

        if (!config.getActive()) {
            throw new IllegalStateException("Connector " + connectorName + " is inactive");
        }

        Connector connector = connectorFactory.getConnector(config.getType());

        // Create Execution Log
        ConnectorExecution execution = ConnectorExecution.builder()
                .connectorConfig(config)
                .startedAt(Instant.now())
                .status("RUNNING")
                .requestPayload(request.getPayload().toString())
                .build();
        execution = executionRepository.save(execution);

        try {
            // Configure Resilience
            Retry retry = getRetry(config);
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(config.getName());

            // Decorate Execution
            Supplier<ConnectorResponse> decoratedSupplier = Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> connector.execute(config, request)));

            // Execute
            ConnectorResponse response = decoratedSupplier.get();

            // Update Log
            execution.setFinishedAt(Instant.now());
            execution.setDurationMs(response.getDurationMs());
            execution.setStatus(response.isSuccess() ? "SUCCESS" : "FAILURE");
            execution.setStatusCode(response.getStatusCode());
            execution.setResponsePayload(response.getResponseBody());
            execution.setErrorMessage(response.getErrorMessage());
            executionRepository.save(execution);

            return response;

        } catch (Exception e) {
            // Handle Resilience Failures (CircuitOpen, MaxRetries exceeded)
            log.error("Execution failed for connector {}: {}", connectorName, e.getMessage());

            execution.setFinishedAt(Instant.now());
            execution.setDurationMs(Instant.now().toEpochMilli() - execution.getStartedAt().toEpochMilli());
            execution.setStatus("FAILURE");
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);

            return ConnectorResponse.builder()
                    .success(false)
                    .statusCode(500)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Retry getRetry(ConnectorConfig config) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getMaxRetries() != null ? config.getMaxRetries() : 3)
                .waitDuration(Duration.ofMillis(config.getRetryBackoffMs() != null ? config.getRetryBackoffMs() : 1000))
                .build();

        return retryRegistry.retry(config.getName(), retryConfig);
    }

    @Transactional
    public ConnectorConfig createConnector(ConnectorConfig config) {
        return configRepository.save(config);
    }
}
