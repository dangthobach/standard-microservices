-- Initialize databases for workflow testing

-- Create separate databases
CREATE DATABASE iam_db;
CREATE DATABASE business_db;
CREATE DATABASE process_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE iam_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE business_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE process_db TO postgres;
