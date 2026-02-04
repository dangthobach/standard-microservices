-- V6: Add Product Approval Workflow Roles
-- Created: 2026-02-04
-- Description: Add ROLE_CHECKER and ROLE_CONFIRMER for product approval workflow

-- Add ROLE_CHECKER
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'ROLE_CHECKER', 'Product approval checker - first level approval', NOW(), NOW()),
    (gen_random_uuid(), 'ROLE_CONFIRMER', 'Product approval confirmer - final level approval', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Grant product read permission to both roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('ROLE_CHECKER', 'ROLE_CONFIRMER')
  AND p.code = 'product:read'
ON CONFLICT DO NOTHING;

-- Log the migration
DO $$
BEGIN
    RAISE NOTICE 'Created workflow approval roles: ROLE_CHECKER and ROLE_CONFIRMER';
END $$;
