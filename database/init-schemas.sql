-- ============================================================================
-- Database Initialization Script for Enterprise Microservices (CORRECTED)
-- ============================================================================
-- Purpose: Create schemas matching ACTUAL entity classes in codebase
-- Database: PostgreSQL 14+
-- Connection: jdbc:postgresql://localhost:5432/postgres
-- Credentials: postgres/postgres
-- ============================================================================

-- ============================================================================
-- 1. CREATE SCHEMAS
-- ============================================================================

DROP SCHEMA IF EXISTS iam_schema CASCADE;
DROP SCHEMA IF EXISTS business_schema CASCADE;

CREATE SCHEMA iam_schema;
COMMENT ON SCHEMA iam_schema IS 'Schema for IAM (Identity and Access Management) Service';

CREATE SCHEMA business_schema;
COMMENT ON SCHEMA business_schema IS 'Schema for Business Service';

-- ============================================================================
-- 2. CREATE USERS (Optional - for security isolation)
-- ============================================================================

DROP USER IF EXISTS iam_user;
DROP USER IF EXISTS business_user;

CREATE USER iam_user WITH PASSWORD 'iam_password_123';
CREATE USER business_user WITH PASSWORD 'business_password_123';

GRANT USAGE, CREATE ON SCHEMA iam_schema TO iam_user;
GRANT USAGE, CREATE ON SCHEMA business_schema TO business_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA iam_schema GRANT ALL ON TABLES TO iam_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA business_schema GRANT ALL ON TABLES TO business_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA iam_schema GRANT ALL ON SEQUENCES TO iam_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA business_schema GRANT ALL ON SEQUENCES TO business_user;

-- ============================================================================
-- 3. IAM SCHEMA TABLES (Match actual Entity classes)
-- ============================================================================

SET search_path TO iam_schema;

-- Users table (matches User.java entity)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- User identification (Keycloak integration)
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    -- Status flags
    enabled BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,

    -- Login tracking
    last_login_at TIMESTAMP,

    -- Audit fields (from SoftDeletableEntity -> AuditableEntity -> BaseEntity)
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete (from SoftDeletableEntity)
    deleted BOOLEAN DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP
);

-- Indexes for users
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_user_deleted ON users(deleted);

-- Roles table (matches Role.java entity)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Role details
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),

    -- Audit fields
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP
);

-- User-Role mapping (Many-to-Many)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Permissions table (matches Permission.java entity)
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Permission details
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),

    -- Audit fields
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP
);

-- ============================================================================
-- 4. BUSINESS SCHEMA TABLES (Sample entities - adjust as needed)
-- ============================================================================

SET search_path TO business_schema;

-- Customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Customer details
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(20),

    -- Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),

    -- Business info
    tax_id VARCHAR(50),
    credit_limit DECIMAL(15, 2) DEFAULT 0,

    -- Audit fields
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_customers_customer_code ON customers(customer_code) WHERE NOT deleted;
CREATE INDEX idx_customers_company_name ON customers(company_name) WHERE NOT deleted;

-- Products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Product details
    product_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),

    -- Pricing
    unit_price DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',

    -- Inventory
    stock_quantity INTEGER DEFAULT 0,
    reorder_level INTEGER DEFAULT 0,

    -- Audit fields
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_products_product_code ON products(product_code) WHERE NOT deleted;
CREATE INDEX idx_products_name ON products(name) WHERE NOT deleted;
CREATE INDEX idx_products_category ON products(category) WHERE NOT deleted;

-- Orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Order details
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    required_date TIMESTAMP,
    shipped_date TIMESTAMP,

    -- Financial
    subtotal DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    shipping_cost DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',

    -- Shipping
    shipping_address VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_country VARCHAR(100),
    shipping_postal_code VARCHAR(20),

    -- Notes
    notes TEXT,

    -- Audit fields
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_orders_order_number ON orders(order_number) WHERE NOT deleted;
CREATE INDEX idx_orders_customer_id ON orders(customer_id) WHERE NOT deleted;
CREATE INDEX idx_orders_order_date ON orders(order_date);

-- Order Items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- References
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,

    -- Item details
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(15, 2) NOT NULL,

    -- Notes
    notes TEXT,

    -- Audit fields
    version INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- ============================================================================
-- 5. INSERT SEED DATA
-- ============================================================================

SET search_path TO iam_schema;

-- Insert default roles (matching actual Role entity)
INSERT INTO roles (id, name, description, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ADMIN', 'System Administrator with full access', CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'DEVELOPER', 'Developer with access to dashboard and APIs', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'SUPPORT', 'Support team with read-only access', CURRENT_TIMESTAMP),
    ('44444444-4444-4444-4444-444444444444', 'USER', 'Regular user with basic access', CURRENT_TIMESTAMP);

-- Insert default permissions (matching actual Permission entity)
INSERT INTO permissions (id, name, description, created_at) VALUES
    (gen_random_uuid(), 'dashboard.view', 'View dashboard metrics', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'users.read', 'Read user information', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'users.write', 'Create and update users', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'users.delete', 'Delete users', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'orders.read', 'Read orders', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'orders.write', 'Create and update orders', CURRENT_TIMESTAMP);

-- NOTE: Test users should be created via Keycloak, not directly in database
-- The users table is synced from Keycloak automatically
-- Example Keycloak user creation via Admin Console or API

SET search_path TO business_schema;

-- Insert sample customers
INSERT INTO customers (id, customer_code, company_name, contact_name, email, phone, city, country, created_at) VALUES
    (gen_random_uuid(), 'CUST001', 'Acme Corporation', 'John Smith', 'john@acme.com', '+1-555-0101', 'New York', 'USA', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CUST002', 'TechCorp Inc', 'Jane Doe', 'jane@techcorp.com', '+1-555-0102', 'San Francisco', 'USA', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CUST003', 'Global Industries', 'Bob Johnson', 'bob@global.com', '+1-555-0103', 'Chicago', 'USA', CURRENT_TIMESTAMP);

-- Insert sample products
INSERT INTO products (id, product_code, name, description, category, unit_price, stock_quantity, created_at) VALUES
    (gen_random_uuid(), 'PROD001', 'Widget A', 'High-quality widget', 'Widgets', 29.99, 100, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'PROD002', 'Gadget B', 'Premium gadget', 'Gadgets', 49.99, 50, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'PROD003', 'Tool C', 'Professional tool', 'Tools', 99.99, 25, CURRENT_TIMESTAMP);

-- ============================================================================
-- 6. GRANT PERMISSIONS TO SERVICE USERS
-- ============================================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA iam_schema TO iam_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA iam_schema TO iam_user;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA business_schema TO business_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA business_schema TO business_user;

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '============================================================================';
    RAISE NOTICE 'Database initialization completed successfully!';
    RAISE NOTICE '============================================================================';
    RAISE NOTICE 'Schemas created:';
    RAISE NOTICE '  - iam_schema (3 tables: users, roles, permissions)';
    RAISE NOTICE '  - business_schema (4 tables: customers, products, orders, order_items)';
    RAISE NOTICE '';
    RAISE NOTICE 'Database users created:';
    RAISE NOTICE '  - iam_user / iam_password_123';
    RAISE NOTICE '  - business_user / business_password_123';
    RAISE NOTICE '';
    RAISE NOTICE 'Seed data:';
    RAISE NOTICE '  - 4 Roles (ADMIN, DEVELOPER, SUPPORT, USER)';
    RAISE NOTICE '  - 6 Permissions (dashboard, users, orders)';
    RAISE NOTICE '  - 3 Sample customers';
    RAISE NOTICE '  - 3 Sample products';
    RAISE NOTICE '';
    RAISE NOTICE 'IMPORTANT: Test users must be created in Keycloak';
    RAISE NOTICE '  Keycloak Admin Console: http://localhost:8180/admin';
    RAISE NOTICE '  Realm: enterprise';
    RAISE NOTICE '  Create users: admin@enterprise.com, developer@enterprise.com';
    RAISE NOTICE '  Assign roles via Keycloak role mapping';
    RAISE NOTICE '';
    RAISE NOTICE 'Connection strings:';
    RAISE NOTICE '  IAM:      jdbc:postgresql://localhost:5432/postgres?currentSchema=iam_schema';
    RAISE NOTICE '  Business: jdbc:postgresql://localhost:5432/postgres?currentSchema=business_schema';
    RAISE NOTICE '============================================================================';
END $$;
