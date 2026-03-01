package com.enterprise.common.audit;

/**
 * Interface for publishing audit events.
 * Implementations can send to Kafka, DB, or Logger.
 */
public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
