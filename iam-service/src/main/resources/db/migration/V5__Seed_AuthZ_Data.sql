-- Seed Data for Dynamic AuthZ
-- Permissions and Endpoint Protections

-- 1. Insert New Permissions (Business Domain)
INSERT INTO permissions (code, resource, action, description, created_by) VALUES
('business:read', 'business', 'read', 'Read Access to Business Data', 'system'),
('order:create', 'order', 'create', 'Create New Orders', 'system'),
('order:read', 'order', 'read', 'View Orders', 'system'),
('product:read', 'product', 'read', 'View Products', 'system')
ON CONFLICT (code) DO NOTHING;

-- 2. Assign Permissions to Roles
-- ADMIN gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN' 
AND p.code IN ('business:read', 'order:create', 'order:read', 'product:read')
ON CONFLICT DO NOTHING;

-- USER gets Read access + Create Order
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('business:read', 'order:create', 'order:read', 'product:read')
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;

-- 3. Insert Endpoint Protections (Policies)

-- Public Endpoints (High Priority)
INSERT INTO endpoint_protections (pattern, method, is_public, priority, active, created_by) VALUES
('/auth/**', '*', TRUE, 100, TRUE, 'system'),
('/login/**', '*', TRUE, 100, TRUE, 'system'),
('/oauth2/**', '*', TRUE, 100, TRUE, 'system'),
('/public/**', '*', TRUE, 100, TRUE, 'system'),
('/actuator/**', '*', TRUE, 100, TRUE, 'system')
ON CONFLICT DO NOTHING; -- No natural key unique constraint, but safe for initial seed

-- Protected Endpoints (Business Service)
INSERT INTO endpoint_protections (pattern, method, permission_code, is_public, priority, active, created_by) VALUES
-- Order Management
('/api/business/orders', 'POST', 'order:create', FALSE, 10, TRUE, 'system'),
('/api/business/orders/**', 'GET', 'order:read', FALSE, 10, TRUE, 'system'),

-- Product Management
('/api/business/products/**', 'GET', 'product:read', FALSE, 10, TRUE, 'system'),

-- User Management (IAM)
('/api/iam/users/**', 'GET', 'user:read', FALSE, 10, TRUE, 'system'),
('/api/iam/users/**', 'POST', 'user:write', FALSE, 10, TRUE, 'system')
;
