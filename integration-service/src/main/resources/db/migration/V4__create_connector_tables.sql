-- Connector Configs Table
CREATE TABLE connector_configs (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    endpoint_url VARCHAR(255) NOT NULL,
    auth_type VARCHAR(20) NOT NULL,
    auth_config TEXT,
    default_headers TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    timeout_seconds INTEGER NOT NULL DEFAULT 30,
    max_retries INTEGER NOT NULL DEFAULT 3,
    retry_backoff_ms BIGINT NOT NULL DEFAULT 1000,
    
    -- AuditableEntity fields
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX idx_connector_name ON connector_configs(name);
CREATE INDEX idx_connector_type ON connector_configs(type);
CREATE INDEX idx_connector_active ON connector_configs(active);

-- Connector Executions Table
CREATE TABLE connector_executions (
    id UUID PRIMARY KEY,
    connector_config_id UUID NOT NULL REFERENCES connector_configs(id),
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    status VARCHAR(20) NOT NULL,
    status_code INTEGER,
    request_payload TEXT,
    response_payload TEXT,
    error_message TEXT,
    
    -- BaseEntity fields
    version BIGINT
);

CREATE INDEX idx_exec_connector_id ON connector_executions(connector_config_id);
CREATE INDEX idx_exec_status ON connector_executions(status);
CREATE INDEX idx_exec_created ON connector_executions(started_at);
