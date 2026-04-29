-- Product History table (explicit history pattern)
-- This table stores state transition snapshots/diffs for Product.

CREATE TABLE IF NOT EXISTS product_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Reference
    entity_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,

    -- Action / status transition
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    current_status VARCHAR(50),

    -- Snapshots (JSON stored as text; can be migrated to jsonb if desired)
    snapshot TEXT,
    diff TEXT,

    -- Metadata
    changed_by VARCHAR(255),
    correlation_id VARCHAR(255),
    ip_address VARCHAR(50),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_product_history_entity_id ON product_history(entity_id);
CREATE INDEX IF NOT EXISTS idx_product_history_created_at ON product_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_history_action ON product_history(action);
