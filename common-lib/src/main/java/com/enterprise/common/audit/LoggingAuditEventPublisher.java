package com.enterprise.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default implementation of AuditEventPublisher that logs events using SLF4J.
 * This is used when no other publisher (like Kafka) is configured.
 * Logs are written with "AUDIT_EVENT: " prefix for easy extraction by log
 * collectors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "kafkaAuditEventPublisher")
public class LoggingAuditEventPublisher implements AuditEventPublisher {

    private final ObjectMapper objectMapper;

    @Override
    public void publish(AuditEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("AUDIT_EVENT: {}", eventJson);
        } catch (Exception e) {
            log.error("Failed to serialize audit event", e);
            log.info("AUDIT_EVENT: {}", event);
        }
    }
}
