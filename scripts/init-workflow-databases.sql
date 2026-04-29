-- Initialize database for workflow testing (single DB, schema-per-service)

-- Create single database
CREATE DATABASE enterprise_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE enterprise_db TO postgres;

-- Schemas are created automatically by Flyway (create-schemas=true) on service startup.
-- If you want to pre-create them manually, run these after connecting to enterprise_db:
--   CREATE SCHEMA IF NOT EXISTS iam_schema;
--   CREATE SCHEMA IF NOT EXISTS business_schema;
--   CREATE SCHEMA IF NOT EXISTS integration_schema;
--   CREATE SCHEMA IF NOT EXISTS flowable;
