-- User Request (Maker/Checker) Implementation
-- Version: 3.0.0
-- Description: Creates user_requests, user_request_history tables and permissions for Maker/Checker workflow

-- ============================================================================
-- 1. USER REQUESTS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User data
    email VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    -- Request creator (Maker)
    request_creator_id VARCHAR(100) NOT NULL,

    -- State machine fields (from StatefulEntity)
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    previous_status VARCHAR(50),
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_change_reason VARCHAR(500),

    -- Audit fields (from SoftDeletableEntity -> AuditableEntity -> BaseEntity)
    version BIGINT DEFAULT 0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP
);

-- Indexes for user_requests (optimized for 1M user scale)
CREATE INDEX IF NOT EXISTS idx_request_status ON user_requests(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_request_creator ON user_requests(request_creator_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_request_email ON user_requests(email) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_request_deleted ON user_requests(deleted);
CREATE INDEX IF NOT EXISTS idx_request_status_changed_at ON user_requests(status_changed_at) WHERE deleted = FALSE;

-- ============================================================================
-- 2. USER REQUEST ROLES TABLE (ElementCollection)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_request_roles (
    request_id UUID NOT NULL REFERENCES user_requests(id) ON DELETE CASCADE,
    role_id UUID NOT NULL,
    PRIMARY KEY (request_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_request_roles_request_id ON user_request_roles(request_id);
CREATE INDEX IF NOT EXISTS idx_request_roles_role_id ON user_request_roles(role_id);

-- ============================================================================
-- 3. USER REQUEST HISTORY TABLE (Audit Log)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_request_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Reference to request
    request_id UUID NOT NULL REFERENCES user_requests(id) ON DELETE CASCADE,

    -- Status transition
    old_status VARCHAR(50),
    new_status VARCHAR(50),

    -- Action performed
    action VARCHAR(50) NOT NULL,

    -- Actor and comment
    actor_id VARCHAR(100) NOT NULL,
    comment VARCHAR(500),

    -- Metadata (JSONB for flexibility)
    metadata JSONB,

    -- Audit fields (from AuditableEntity -> BaseEntity)
    version BIGINT DEFAULT 0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user_request_history (optimized for fast history reads)
CREATE INDEX IF NOT EXISTS idx_history_request_id ON user_request_history(request_id);
CREATE INDEX IF NOT EXISTS idx_history_action ON user_request_history(action);
CREATE INDEX IF NOT EXISTS idx_history_actor_id ON user_request_history(actor_id);
CREATE INDEX IF NOT EXISTS idx_history_created_at ON user_request_history(created_at);

-- ============================================================================
-- 4. TRIGGERS FOR AUTO-UPDATE updated_at
-- ============================================================================

DROP TRIGGER IF EXISTS update_user_requests_updated_at ON user_requests;
CREATE TRIGGER update_user_requests_updated_at BEFORE UPDATE ON user_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_user_request_history_updated_at ON user_request_history;
CREATE TRIGGER update_user_request_history_updated_at BEFORE UPDATE ON user_request_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 5. INSERT PERMISSIONS
-- ============================================================================

INSERT INTO permissions (code, resource, action, description, created_by) VALUES
('USER_REQUEST_CREATE', 'user_request', 'create', 'Create and update user requests (Maker)', 'system'),
('USER_REQUEST_VIEW', 'user_request', 'view', 'View user requests (Both Maker and Checker)', 'system'),
('USER_REQUEST_APPROVE', 'user_request', 'approve', 'Approve and reject user requests (Checker)', 'system')
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- 6. ASSIGN PERMISSIONS TO ROLES
-- ============================================================================

-- ADMIN: All permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('USER_REQUEST_CREATE', 'USER_REQUEST_VIEW', 'USER_REQUEST_APPROVE')
ON CONFLICT DO NOTHING;

-- MANAGER: Create, View, Approve
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('USER_REQUEST_CREATE', 'USER_REQUEST_VIEW', 'USER_REQUEST_APPROVE')
WHERE r.name = 'MANAGER'
ON CONFLICT DO NOTHING;

-- USER: Create and View only (cannot approve)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('USER_REQUEST_CREATE', 'USER_REQUEST_VIEW')
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;

