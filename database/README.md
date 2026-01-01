# Database Setup Guide

## Overview

This directory contains SQL scripts to initialize PostgreSQL database for the Enterprise Microservices Platform.

## Architecture

**Single Database, Multiple Schemas Approach:**
- Database: `postgres` (existing)
- Schema 1: `iam_schema` (IAM Service)
- Schema 2: `business_schema` (Business Service)

This approach provides:
- ✅ **Logical isolation** between services
- ✅ **Single database instance** (simpler ops)
- ✅ **Easy backups** (one database)
- ✅ **Referential integrity** (if needed cross-schema)

---

## Prerequisites

- PostgreSQL 14+ installed and running
- Connection details:
  - Host: `localhost`
  - Port: `5432`
  - Database: `postgres`
  - Username: `postgres`
  - Password: `postgres`

---

## Quick Start

### 1. Run Initialization Script

**Option A: Using automated setup script (Recommended):**
```bash
# Windows
setup-database.bat

# Linux/Mac
./setup-database.sh
```

**Option B: Using psql command line:**
```bash
psql -U postgres -d postgres -f init-schemas-fixed.sql
```

**Option C: Using psql interactive mode:**
```bash
psql -U postgres -d postgres

postgres=# \i init-schemas-fixed.sql
```

**Option D: Using PostgreSQL GUI (pgAdmin, DBeaver, etc.):**
1. Connect to database `postgres`
2. Open `init-schemas-fixed.sql`
3. Execute script

---

## What Gets Created

### Schemas
- `iam_schema` - IAM Service tables
- `business_schema` - Business Service tables

### Database Users
- `iam_user` / `iam_password_123` - For IAM Service
- `business_user` / `business_password_123` - For Business Service

### IAM Schema Tables
1. **users** - User accounts
2. **roles** - Role definitions (ADMIN, DEVELOPER, SUPPORT, USER)
3. **user_roles** - User-Role mapping
4. **permissions** - Permission definitions
5. **role_permissions** - Role-Permission mapping
6. **refresh_tokens** - JWT refresh token storage
7. **audit_logs** - Security event logging

### Business Schema Tables
1. **customers** - Customer management
2. **products** - Product catalog
3. **orders** - Order headers
4. **order_items** - Order line items

### Seed Data

**Test Users (password: `password123`):**
| Username | Email | Role | Purpose |
|----------|-------|------|---------|
| admin | admin@enterprise.com | ADMIN | Full system access |
| developer | developer@enterprise.com | DEVELOPER | Dashboard + API access |
| testuser | user@enterprise.com | USER | Basic user access |

**Sample Business Data:**
- 3 Customers (CUST001, CUST002, CUST003)
- 3 Products (PROD001, PROD002, PROD003)

---

## Service Configuration

### IAM Service Configuration

**File:** `iam-service/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: iam-service

  datasource:
    url: jdbc:postgresql://localhost:5432/postgres?currentSchema=iam_schema
    username: iam_user
    password: iam_password_123
    driver-class-name: org.postgresql.Driver

    # HikariCP connection pool
    hikari:
      maximum-pool-size: 100
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: IAM-HikariPool

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Don't auto-create tables, use SQL scripts
    properties:
      hibernate:
        default_schema: iam_schema
        format_sql: true
        show_sql: false
    show-sql: false
```

### Business Service Configuration

**File:** `business-service/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: business-service

  datasource:
    url: jdbc:postgresql://localhost:5432/postgres?currentSchema=business_schema
    username: business_user
    password: business_password_123
    driver-class-name: org.postgresql.Driver

    # HikariCP connection pool
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: Business-HikariPool

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Don't auto-create tables, use SQL scripts
    properties:
      hibernate:
        default_schema: business_schema
        format_sql: true
        show_sql: false
    show-sql: false
```

---

## Verification

### 1. Verify Schemas Created

```sql
-- Connect to database
psql -U postgres -d postgres

-- List all schemas
\dn

-- Expected output:
-- Name            | Owner
-- ----------------+---------
-- iam_schema      | postgres
-- business_schema | postgres
```

### 2. Verify Tables Created

```sql
-- Check IAM schema tables
\dt iam_schema.*

-- Expected: users, roles, user_roles, permissions, role_permissions, refresh_tokens, audit_logs

-- Check Business schema tables
\dt business_schema.*

-- Expected: customers, products, orders, order_items
```

### 3. Verify Users Created

```sql
\du

-- Expected users: postgres, iam_user, business_user
```

### 4. Verify Seed Data

```sql
-- Check test users
SELECT username, email, status
FROM iam_schema.users;

-- Expected: admin, developer, testuser

-- Check roles
SELECT name, description
FROM iam_schema.roles;

-- Expected: ADMIN, DEVELOPER, SUPPORT, USER

-- Check sample customers
SELECT customer_code, company_name, status
FROM business_schema.customers;

-- Expected: CUST001 (Acme), CUST002 (TechCorp), CUST003 (Global)
```

### 5. Test Database Connectivity from Services

**Start IAM Service and check logs:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
DataSource detected - database metrics will be reported
```

**Verify metrics in Redis:**
```bash
redis-cli

# Check database metrics
KEYS dashboard:service:*:db

# Should show:
# 1) "dashboard:service:iam-service:db"
# 2) "dashboard:service:business-service:db"

GET dashboard:service:iam-service:db

# Should show JSON with:
# {"serviceName":"iam-service","connections":10,"maxConnections":100,...}
```

---

## Connection Strings Reference

### For Service Configuration (application.yml)

**IAM Service:**
```
jdbc:postgresql://localhost:5432/postgres?currentSchema=iam_schema
User: iam_user
Password: iam_password_123
```

**Business Service:**
```
jdbc:postgresql://localhost:5432/postgres?currentSchema=business_schema
User: business_user
Password: business_password_123
```

### For Admin Access (psql/pgAdmin)

**Connect to iam_schema:**
```bash
psql -U postgres -d postgres
\c postgres
SET search_path TO iam_schema;
```

**Connect to business_schema:**
```bash
psql -U postgres -d postgres
\c postgres
SET search_path TO business_schema;
```

---

## Maintenance

### Reset Database (Clean Re-initialization)

```bash
# WARNING: This will DELETE ALL DATA!
# Option 1: Using automated script
setup-database.bat  # Windows
./setup-database.sh # Linux/Mac

# Option 2: Direct psql
psql -U postgres -d postgres -f init-schemas-fixed.sql
```

The script automatically:
1. Drops existing schemas (CASCADE)
2. Drops existing users
3. Recreates everything
4. Inserts fresh seed data

### Backup Individual Schema

```bash
# Backup IAM schema only
pg_dump -U postgres -n iam_schema postgres > iam_schema_backup.sql

# Backup Business schema only
pg_dump -U postgres -n business_schema postgres > business_schema_backup.sql
```

### Restore Individual Schema

```bash
# Restore IAM schema
psql -U postgres -d postgres < iam_schema_backup.sql
```

---

## Security Notes

### Production Deployment

**⚠️ IMPORTANT: Change default passwords in production!**

```sql
-- Change iam_user password
ALTER USER iam_user WITH PASSWORD 'STRONG_PASSWORD_HERE';

-- Change business_user password
ALTER USER business_user WITH PASSWORD 'STRONG_PASSWORD_HERE';
```

**Update application.yml:**
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}  # Use environment variable
```

### Schema Isolation

Services are isolated at schema level:
- ✅ `iam_user` can ONLY access `iam_schema`
- ✅ `business_user` can ONLY access `business_schema`
- ✅ Cross-schema queries require `postgres` superuser

### Test User Passwords

Test users have password: `password123` (bcrypt hashed)

**⚠️ For production:**
1. Delete test users
2. Create real users via IAM Service API
3. Enforce strong password policy

---

## Troubleshooting

### Error: "schema does not exist"

**Problem:** Service can't find schema

**Solution:** Check `currentSchema` in JDBC URL
```
jdbc:postgresql://localhost:5432/postgres?currentSchema=iam_schema
```

### Error: "permission denied for schema"

**Problem:** User doesn't have schema access

**Solution:** Grant permissions
```sql
GRANT USAGE, CREATE ON SCHEMA iam_schema TO iam_user;
GRANT ALL ON ALL TABLES IN SCHEMA iam_schema TO iam_user;
```

### Error: "HikariCP pool not starting"

**Problem:** Database connection failed

**Solution:** Verify:
1. PostgreSQL is running: `pg_ctl status`
2. Credentials are correct
3. Database `postgres` exists
4. User has login privileges

### Dashboard shows no database metrics

**Problem:** MetricsReporter not detecting DataSource

**Solution:**
1. Check service logs for "DataSource detected" message
2. Verify HikariCP dependency in pom.xml
3. Ensure `metrics.reporter.enabled=true` in application.yml

---

## Files in This Directory

| File | Purpose |
|------|---------|
| `init-schemas-fixed.sql` | Main initialization script (creates schemas, tables, seed data matching actual entity classes) |
| `setup-database.bat` | Automated setup script for Windows |
| `setup-database.sh` | Automated setup script for Linux/Mac |
| `README.md` | This file - setup guide and reference |

---

## Next Steps

After database setup:

1. ✅ Update service configurations (`application.yml`)
2. ✅ Start services and verify database connectivity
3. ✅ Check Redis for database metrics
4. ✅ Test Dashboard API: `GET /api/v1/dashboard/database`
5. ✅ Verify frontend shows IAM and Business database panels

---

**Status:** ✅ Ready for development and testing
**Production Ready:** ⚠️ Change passwords and remove test data first!
