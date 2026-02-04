-- V7: Seed Test Users for Product Approval Workflow
-- Created: 2026-02-04
-- Description: Create test users with ROLE_CHECKER and ROLE_CONFIRMER for E2E testing

-- NOTE: In production, users would be created through Keycloak.
-- This migration is for test/dev environments only.

-- Insert test users (using placeholder keycloak_ids)
INSERT INTO users (id, keycloak_id, email, first_name, last_name, enabled, email_verified, created_by)
VALUES 
    -- Checker users
    (gen_random_uuid(), 'test-checker-1-keycloak-id', 'checker1@test.com', 'Alice', 'Checker', TRUE, TRUE, 'system'),
    (gen_random_uuid(), 'test-checker-2-keycloak-id', 'checker2@test.com', 'Bob', 'Checker', TRUE, TRUE, 'system'),
    
    -- Confirmer users  
    (gen_random_uuid(), 'test-confirmer-1-keycloak-id', 'confirmer1@test.com', 'Charlie', 'Confirmer', TRUE, TRUE, 'system'),
    (gen_random_uuid(), 'test-confirmer-2-keycloak-id', 'confirmer2@test.com', 'Diana', 'Confirmer', TRUE, TRUE, 'system'),
    
    -- Admin with both roles (for testing)
    (gen_random_uuid(), 'test-admin-workflow-keycloak-id', 'admin.workflow@test.com', 'Admin', 'Workflow', TRUE, TRUE, 'system')
ON CONFLICT (email) DO NOTHING;

-- Assign ROLE_CHECKER to checker users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email IN ('checker1@test.com', 'checker2@test.com')
  AND r.name = 'ROLE_CHECKER'
ON CONFLICT DO NOTHING;

-- Assign ROLE_CONFIRMER to confirmer users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email IN ('confirmer1@test.com', 'confirmer2@test.com')
  AND r.name = 'ROLE_CONFIRMER'
ON CONFLICT DO NOTHING;

-- Assign both roles to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin.workflow@test.com'
  AND r.name IN ('ROLE_CHECKER', 'ROLE_CONFIRMER', 'ADMIN')
ON CONFLICT DO NOTHING;

-- Log the test users created
DO $$
DECLARE
    checker_count INT;
    confirmer_count INT;
BEGIN
    SELECT COUNT(*) INTO checker_count 
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    WHERE r.name = 'ROLE_CHECKER';
    
    SELECT COUNT(*) INTO confirmer_count
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    WHERE r.name = 'ROLE_CONFIRMER';
    
    RAISE NOTICE 'Created test users - CHECKER: %, CONFIRMER: %', checker_count, confirmer_count;
END $$;
