-- Create Products Table
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    price DECIMAL(19, 2) NOT NULL,
    category VARCHAR(50),
    stock_quantity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    process_instance_id VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    
    -- StatefulEntity fields
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX idx_product_sku ON products(sku);
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_product_status ON products(status);
CREATE INDEX idx_product_deleted ON products(deleted);

-- Create Product Attachments Table
CREATE TABLE product_attachments (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    
    -- AuditableEntity fields
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX idx_attachment_product ON product_attachments(product_id);
CREATE INDEX idx_attachment_created ON product_attachments(created_at);
