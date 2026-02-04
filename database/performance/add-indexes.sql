-- Performance Indexes for Business Service Database
-- These indexes significantly improve query performance for common operations

-- ==================== PRODUCTS TABLE ====================

-- Index for status-based queries (most common filter)
CREATE INDEX IF NOT EXISTS idx_products_status 
ON products(status) 
WHERE deleted_at IS NULL;

-- Index for active products
CREATE INDEX IF NOT EXISTS idx_products_active 
ON products(active) 
WHERE deleted_at IS NULL;

-- Index for process instance lookups
CREATE INDEX IF NOT EXISTS idx_products_process_instance 
ON products(process_instance_id) 
WHERE process_instance_id IS NOT NULL;

-- Index for recent products (ordered by creation date)
CREATE INDEX IF NOT EXISTS idx_products_created_at 
ON products(created_at DESC) 
WHERE deleted_at IS NULL;

-- Composite index for common query pattern (status + active)
CREATE INDEX IF NOT EXISTS idx_products_status_active 
ON products(status, active) 
WHERE deleted_at IS NULL;

-- Index for SKU lookups (unique constraint already provides this)
-- CREATE UNIQUE INDEX idx_products_sku ON products(sku) WHERE deleted_at IS NULL;

-- Index for category filtering
CREATE INDEX IF NOT EXISTS idx_products_category 
ON products(category) 
WHERE deleted_at IS NULL AND category IS NOT NULL;

-- Partial index for pending approvals (hot query)
CREATE INDEX IF NOT EXISTS idx_products_pending_approval 
ON products(id, created_at) 
WHERE status IN ('PENDING_APPROVAL', 'PENDING_CONFIRMATION') 
  AND deleted_at IS NULL;

-- ==================== ANALYZE TABLES ====================

-- Update statistics for query planner
ANALYZE products;

-- ==================== QUERY PERFORMANCE STATS ====================

-- Enable query statistics (if not already enabled)
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- View slow queries (requires pg_stat_statements)
-- SELECT 
--     query,
--     calls,
--     total_exec_time,
--     mean_exec_time,
--     max_exec_time
-- FROM pg_stat_statements
-- WHERE query LIKE '%products%'
-- ORDER BY mean_exec_time DESC
-- LIMIT 10;
