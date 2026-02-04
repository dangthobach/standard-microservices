-- Create Audit Log Table
-- Tracks all important operations for compliance and debugging

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_username ON audit_log(username);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_entity_type ON audit_log(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_log(created_at DESC);

-- Composite index for common query (entity type + entity ID)
CREATE INDEX IF NOT EXISTS idx_audit_entity_lookup 
ON audit_log(entity_type, entity_id, created_at DESC);

-- Comments
COMMENT ON TABLE audit_log IS 'Audit trail for all important system operations';
COMMENT ON COLUMN audit_log.username IS 'Username of the user who performed the action';
COMMENT ON COLUMN audit_log.action IS 'Action performed (e.g., CREATE_PRODUCT, UPDATE_PRODUCT)';
COMMENT ON COLUMN audit_log.entity_type IS 'Type of entity (e.g., PRODUCT, USER)';
COMMENT ON COLUMN audit_log.entity_id IS 'ID of the entity that was modified';
COMMENT ON COLUMN audit_log.old_value IS 'Previous value (for UPDATE operations)';
COMMENT ON COLUMN audit_log.new_value IS 'New value after the operation';
COMMENT ON COLUMN audit_log.ip_address IS 'IP address of the request';
COMMENT ON COLUMN audit_log.user_agent IS 'User agent of the request';
