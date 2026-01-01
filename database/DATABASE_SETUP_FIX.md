# Database Setup Fix - Entity Mapping Corrections

## Problem Identified

The original `init-schemas.sql` script created tables that **did NOT match** the actual Java entity classes in the codebase, causing SQL errors:

```
[42701] ERROR: column "state" specified more than once
```

## Root Cause Analysis

### Original SQL Script Issues:

1. **Users Table Mismatch:**
   - ❌ Script had: `username`, `password_hash`, `status`, `state`
   - ✅ Actual User.java has: `keycloak_id`, `email`, `first_name`, `last_name`, `enabled`, `email_verified`

2. **Authentication Architecture:**
   - ❌ Script assumed local username/password authentication
   - ✅ System uses **Keycloak** for authentication (external identity provider)

3. **Entity Hierarchy Misunderstanding:**
   - ❌ Script added both `status` and `state` fields (duplicate)
   - ✅ StatefulEntity only provides `status` field
   - ✅ Base entity hierarchy: BaseEntity → AuditableEntity → SoftDeletableEntity

## Solution: init-schemas-fixed.sql

Created corrected SQL script that **exactly matches** actual entity classes:

### Users Table (Corrected)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- User identification (Keycloak integration)
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,  -- Maps to User.keycloakId
    email VARCHAR(100) NOT NULL UNIQUE,         -- Maps to User.email
    first_name VARCHAR(100) NOT NULL,           -- Maps to User.firstName
    last_name VARCHAR(100) NOT NULL,            -- Maps to User.lastName

    -- Status flags
    enabled BOOLEAN NOT NULL DEFAULT true,      -- Maps to User.enabled
    email_verified BOOLEAN NOT NULL DEFAULT false, -- Maps to User.emailVerified

    -- Login tracking
    last_login_at TIMESTAMP,                    -- Maps to User.lastLoginAt

    -- Audit fields (from SoftDeletableEntity → AuditableEntity → BaseEntity)
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
```

### Key Changes:

| Issue | Before | After |
|-------|--------|-------|
| Authentication | username/password_hash | keycloak_id (Keycloak integration) |
| User fields | username, password_hash | email, first_name, last_name |
| Status tracking | status + state (duplicate!) | Only audit fields from base entities |
| User management | Local user table | Keycloak-managed users |

## Entity Class Reference

### User.java Entity Structure:

```java
@Entity
@Table(name = "users", schema = "iam_schema")
public class User extends SoftDeletableEntity {
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;  // ← Keycloak user ID

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Inherits from SoftDeletableEntity:
    // - deleted, deleted_by, deleted_at
    // - version, created_by, created_at, updated_by, updated_at
}
```

## How to Use the Fixed Script

### Option 1: Automated Setup (Recommended)

**Windows:**
```bash
cd database
setup-database.bat
```

**Linux/Mac:**
```bash
cd database
chmod +x setup-database.sh
./setup-database.sh
```

### Option 2: Manual psql Execution

```bash
psql -U postgres -d postgres -f init-schemas-fixed.sql
```

### Option 3: Database GUI Tool

1. Connect to: `jdbc:postgresql://localhost:5432/postgres`
2. Open file: `init-schemas-fixed.sql`
3. Execute script

## Verification After Setup

### 1. Check Schemas Created

```sql
\dn

-- Expected output:
-- Name            | Owner
-- ----------------+---------
-- iam_schema      | postgres
-- business_schema | postgres
```

### 2. Verify Users Table Structure

```sql
\d iam_schema.users

-- Should show columns:
-- id, keycloak_id, email, first_name, last_name,
-- enabled, email_verified, last_login_at,
-- version, created_by, created_at, updated_by, updated_at,
-- deleted, deleted_by, deleted_at
```

### 3. Check Seed Data

```sql
SELECT name, description FROM iam_schema.roles;

-- Expected roles:
-- ADMIN, DEVELOPER, SUPPORT, USER
```

## Important Notes

### Test Users Must Be Created in Keycloak

⚠️ **Users are NOT created in the database directly!**

The database script creates the roles and permissions, but **test users must be created in Keycloak**:

1. Open Keycloak Admin Console: http://localhost:8180/admin
2. Select realm: `enterprise`
3. Create users:
   - admin@enterprise.com (assign ADMIN role)
   - developer@enterprise.com (assign DEVELOPER role)
   - user@enterprise.com (assign USER role)
4. Set passwords for each user

When users log in via Keycloak, the IAM Service will automatically sync them to the `users` table.

### Why Keycloak?

- ✅ Centralized authentication (OAuth2/OIDC)
- ✅ Single Sign-On (SSO)
- ✅ Social login support
- ✅ MFA/2FA support
- ✅ User federation (LDAP/AD)
- ✅ Password policies and management

## Files Updated

1. ✅ **init-schemas-fixed.sql** - Corrected SQL script matching actual entities
2. ✅ **setup-database.bat** - Updated to use fixed script
3. ✅ **setup-database.sh** - Updated to use fixed script
4. ✅ **README.md** - Updated documentation with correct file references

## Next Steps

After running the fixed database setup:

1. ✅ Verify schemas created successfully
2. ✅ Update IAM Service configuration (application.yml):
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/postgres?currentSchema=iam_schema
       username: iam_user
       password: iam_password_123
   ```
3. ✅ Update Business Service configuration (application.yml):
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/postgres?currentSchema=business_schema
       username: business_user
       password: business_password_123
   ```
4. ✅ Create test users in Keycloak Admin Console
5. ✅ Start services and verify database connectivity
6. ✅ Check dashboard for distributed database metrics

## Troubleshooting

### If You Still Get Column Errors

Check that you're running the **FIXED** script:
```bash
# ❌ Wrong file
psql -f init-schemas.sql

# ✅ Correct file
psql -f init-schemas-fixed.sql
```

### If Tables Already Exist

The script will automatically drop and recreate schemas:
```sql
DROP SCHEMA IF EXISTS iam_schema CASCADE;
DROP SCHEMA IF EXISTS business_schema CASCADE;
```

This ensures a clean setup every time.

---

**Status:** ✅ Database setup script corrected and verified against actual entity classes
**Ready for:** Production database initialization
