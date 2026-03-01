package com.enterprise.integration.service;

import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.integration.entity.WebhookEndpoint;
import com.enterprise.integration.entity.WebhookEvent;
import com.enterprise.integration.repository.WebhookEndpointRepository;
import com.enterprise.integration.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookEventRepository eventRepository;
    private final WebhookSignatureVerifier signatureVerifier;

    /**
     * Process an incoming webhook request.
     */
    @Transactional
    public void processWebhook(String path, String payload, Map<String, String> headers) {
        WebhookEndpoint endpoint = endpointRepository.findByPath(path)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "path", path));

        if (!endpoint.getActive()) {
            log.warn("Received webhook for inactive endpoint: {}", path);
            saveEvent(endpoint, payload, headers, "IGNORED", "Endpoint inactive", 0L);
            return;
        }

        long startTime = System.currentTimeMillis();
        String status = "PROCESSED";
        String error = null;

        try {
            // Verify Signature
            if (endpoint.getSecret() != null) {
                String signatureHeader = endpoint.getHeaderName() != null ? endpoint.getHeaderName()
                        : "X-Hub-Signature-256";
                String signature = headers.get(signatureHeader.toLowerCase()); // Headers keys are lower-cased by Spring
                                                                               // often

                // Try case-insensitive lookup
                if (signature == null) {
                    for (String key : headers.keySet()) {
                        if (key.equalsIgnoreCase(signatureHeader)) {
                            signature = headers.get(key);
                            break;
                        }
                    }
                }

                if (!signatureVerifier.verifySignature(payload, signature, endpoint.getSecret())) {
                    log.warn("Invalid signature for webhook: {}", path);
                    status = "FAILED";
                    error = "Invalid Signature";
                    return; // Don't process further
                }
            }

            // TODO: Parse payload and route event (send to Kafka/RabbitMQ)
            log.info("Successfully processed webhook for path: {}", path);

        } catch (Exception e) {
            log.error("Error processing webhook: {}", path, e);
            status = "FAILED";
            error = e.getMessage();
        } finally {
            saveEvent(endpoint, payload, headers, status, error, System.currentTimeMillis() - startTime);
        }
    }

    private void saveEvent(WebhookEndpoint endpoint, String payload, Map<String, String> headers,
            String status, String error, long durationMs) {
        WebhookEvent event = WebhookEvent.builder()
                .webhookEndpoint(endpoint)
                .receivedAt(Instant.now())
                .payload(payload)
                .headers(headers.toString())
                .status(status)
                .processingExcepion(error)
                .processingTimeMs(durationMs)
                .build();

        eventRepository.save(event);
    }

    @Transactional
    public WebhookEndpoint createEndpoint(WebhookEndpoint endpoint) {
        return endpointRepository.save(endpoint);
    }
}
