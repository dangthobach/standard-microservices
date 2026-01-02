-- Initialize database with multiple schemas for microservices
-- This script runs automatically when PostgreSQL container starts

-- Create schemas for different services
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS business;
CREATE SCHEMA IF NOT EXISTS process;

-- Set search path to include all schemas
ALTER DATABASE enterprise_db SET search_path TO iam,business,process,public;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA iam TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA business TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA process TO postgres;

-- Log completion
DO $$
BEGIN
  RAISE NOTICE 'Schemas created successfully: iam, business, process';
END $$;
