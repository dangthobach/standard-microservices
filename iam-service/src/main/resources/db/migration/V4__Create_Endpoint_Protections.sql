-- Create endpoint_protections table
-- Dynamic authorization rules for API Gateway
-- Supports 1M CCU by caching these rules in Gateway memory

CREATE TABLE IF NOT EXISTS endpoint_protections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pattern VARCHAR(200) NOT NULL,
    method VARCHAR(10) NOT NULL,
    permission_code VARCHAR(100),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit columns
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Indexes for performance
-- Used by IAM Service to fetch active rules sorted by priority
CREATE INDEX IF NOT EXISTS idx_ep_active_priority ON endpoint_protections(active, priority DESC) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ep_pattern ON endpoint_protections(pattern) WHERE deleted = FALSE;

-- Trigger for updated_at
DROP TRIGGER IF EXISTS update_endpoint_protections_updated_at ON endpoint_protections;
CREATE TRIGGER update_endpoint_protections_updated_at BEFORE UPDATE ON endpoint_protections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
