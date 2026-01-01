#!/bin/bash
# ============================================================================
# Database Setup Script for Enterprise Microservices
# ============================================================================
# Purpose: Initialize PostgreSQL database with IAM and Business schemas
# Usage: ./setup-database.sh
# ============================================================================

set -e  # Exit on error

echo "============================================================================"
echo "Enterprise Microservices - Database Setup"
echo "============================================================================"
echo ""

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-postgres}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

echo "Database Configuration:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Check if PostgreSQL is running
echo "Checking PostgreSQL connection..."
if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c '\q' 2>/dev/null; then
    echo "❌ ERROR: Cannot connect to PostgreSQL"
    echo ""
    echo "Please ensure:"
    echo "  1. PostgreSQL is running"
    echo "  2. Connection details are correct"
    echo "  3. User '$DB_USER' has CREATE privileges"
    echo ""
    exit 1
fi
echo "✅ PostgreSQL connection successful"
echo ""

# Confirm before proceeding
echo "⚠️  WARNING: This will DROP and RECREATE schemas:"
echo "  - iam_schema (all data will be lost)"
echo "  - business_schema (all data will be lost)"
echo ""
read -p "Continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Setup cancelled."
    exit 0
fi
echo ""

# Run initialization script
echo "Running database initialization script..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f init-schemas-fixed.sql

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo "============================================================================"
    echo "✅ Database setup completed successfully!"
    echo "============================================================================"
    echo ""
    echo "Schemas created:"
    echo "  ✅ iam_schema"
    echo "  ✅ business_schema"
    echo ""
    echo "Database users created:"
    echo "  ✅ iam_user / iam_password_123"
    echo "  ✅ business_user / business_password_123"
    echo ""
    echo "Test accounts (password: password123):"
    echo "  ✅ admin@enterprise.com (ADMIN)"
    echo "  ✅ developer@enterprise.com (DEVELOPER)"
    echo "  ✅ user@enterprise.com (USER)"
    echo ""
    echo "Next steps:"
    echo "  1. Update service application.yml files"
    echo "  2. Start IAM Service"
    echo "  3. Start Business Service"
    echo "  4. Verify dashboard database metrics"
    echo ""
    echo "Connection strings:"
    echo "  IAM:      jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME?currentSchema=iam_schema"
    echo "  Business: jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME?currentSchema=business_schema"
    echo "============================================================================"
else
    echo ""
    echo "❌ ERROR: Database setup failed!"
    echo "Please check the error messages above."
    exit 1
fi
