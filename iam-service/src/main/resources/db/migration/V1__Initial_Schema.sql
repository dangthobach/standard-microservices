-- IAM Service Initial Schema
-- Version: 1.0.0
-- Description: Creates users, roles, permissions tables with optimized indexes for 1M CCU

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP
);

-- Create permissions table
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP
);

-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(100),
    deleted_at TIMESTAMP
);

-- Create role_permissions join table
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Create user_roles join table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Create optimized indexes for high-concurrency queries
-- User indexes (critical for authentication flow)
CREATE INDEX idx_user_email ON users(email) WHERE deleted = FALSE;
CREATE INDEX idx_user_keycloak_id ON users(keycloak_id) WHERE deleted = FALSE;
CREATE INDEX idx_user_deleted ON users(deleted);
CREATE INDEX idx_user_enabled ON users(enabled) WHERE deleted = FALSE;

-- Role indexes
CREATE INDEX idx_role_name ON roles(name) WHERE deleted = FALSE;
CREATE INDEX idx_role_deleted ON roles(deleted);

-- Permission indexes
CREATE INDEX idx_permission_name ON permissions(name) WHERE deleted = FALSE;
CREATE INDEX idx_permission_resource_action ON permissions(resource, action) WHERE deleted = FALSE;
CREATE INDEX idx_permission_deleted ON permissions(deleted);

-- Join table indexes for efficient lookups
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to auto-update updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_permissions_updated_at BEFORE UPDATE ON permissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default roles
INSERT INTO roles (name, description, created_by) VALUES
('ADMIN', 'System Administrator with full access', 'system'),
('USER', 'Standard user with basic access', 'system'),
('MANAGER', 'Manager with elevated permissions', 'system');

-- Insert default permissions
INSERT INTO permissions (name, resource, action, description, created_by) VALUES
('user:read', 'user', 'read', 'View user information', 'system'),
('user:write', 'user', 'write', 'Create and update users', 'system'),
('user:delete', 'user', 'delete', 'Delete users', 'system'),
('role:read', 'role', 'read', 'View roles', 'system'),
('role:write', 'role', 'write', 'Create and update roles', 'system'),
('role:delete', 'role', 'delete', 'Delete roles', 'system'),
('permission:read', 'permission', 'read', 'View permissions', 'system'),
('permission:write', 'permission', 'write', 'Create and update permissions', 'system'),
('permission:delete', 'permission', 'delete', 'Delete permissions', 'system');

-- Assign permissions to ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- Assign basic permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('user:read')
WHERE r.name = 'USER';

-- Assign permissions to MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('user:read', 'user:write', 'role:read')
WHERE r.name = 'MANAGER';
