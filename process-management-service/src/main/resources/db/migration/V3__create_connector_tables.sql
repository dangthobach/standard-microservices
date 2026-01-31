CREATE TABLE flowable.connector_definition (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50),
    configuration TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
