-- Webhook Endpoints Table
CREATE TABLE webhook_endpoints (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    path VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    secret VARCHAR(255),
    header_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    events TEXT,
    
    -- AuditableEntity fields
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX idx_webhook_path ON webhook_endpoints(path);
CREATE INDEX idx_webhook_active ON webhook_endpoints(active);

-- Webhook Events Table
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    webhook_endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id),
    received_at TIMESTAMP NOT NULL,
    payload TEXT,
    headers TEXT,
    status VARCHAR(20) NOT NULL,
    processing_excepion TEXT,
    processing_time_ms BIGINT NOT NULL,
    
    -- BaseEntity fields
    version BIGINT
);

CREATE INDEX idx_event_endpoint_id ON webhook_events(webhook_endpoint_id);
CREATE INDEX idx_event_status ON webhook_events(status);
CREATE INDEX idx_event_received ON webhook_events(received_at);
