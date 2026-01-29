-- V1: Initialize 'flowable' schema for Flowable tables
CREATE SCHEMA IF NOT EXISTS flowable;

-- Ensure the postgres user has full access to the schema
GRANT ALL PRIVILEGES ON SCHEMA flowable TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA flowable TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA flowable TO postgres;
