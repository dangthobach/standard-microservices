# Test Users for Product Approval Workflow

This directory contains scripts and migrations to create test users for the product approval workflow.

## Test Users Created

### Checker Users (ROLE_CHECKER)
| Username | Email | Name | Password | Role |
|----------|-------|------|----------|------|
| checker1 | checker1@test.com | Alice Checker | Checker123! | ROLE_CHECKER |
| checker2 | checker2@test.com | Bob Checker | Checker123! | ROLE_CHECKER |

### Confirmer Users (ROLE_CONFIRMER)
| Username | Email | Name | Password | Role |
|----------|-------|------|----------|------|
| confirmer1 | confirmer1@test.com | Charlie Confirmer | Confirmer123! | ROLE_CONFIRMER |
| confirmer2 | confirmer2@test.com | Diana Confirmer | Confirmer123! | ROLE_CONFIRMER |

### Admin User (Both Roles)
| Username | Email | Name | Password | Roles |
|----------|-------|------|----------|-------|
| admin.workflow | admin.workflow@test.com | Admin Workflow | AdminWorkflow123! | ADMIN, ROLE_CHECKER, ROLE_CONFIRMER |

## Setup Methods

### Method 1: Database Migration (Dev/Test)

**For local development without Keycloak**

The migration `V7__Seed_Test_Users_For_Workflow.sql` will automatically create these users when the IAM service starts.

**Note:** Uses placeholder keycloak_ids. Good for testing without full Keycloak setup.

```bash
cd iam-service
mvn spring-boot:run
# Users will be created automatically via Flyway migration
```

### Method 2: Keycloak Admin API (Production-like)

**For testing with full Keycloak integration**

Use the `create-keycloak-users.http` file with your HTTP client (Postman, Insomnia, IntelliJ).

**Prerequisites:**
1. Keycloak is running on port 8180
2. Admin credentials: admin/admin
3. Realm 'enterprise' exists
4. Roles ROLE_CHECKER and ROLE_CONFIRMER are created in Keycloak

**Steps:**
```http
1. Get admin token
2. Create each user via Keycloak Admin API
3. Assign realm roles to users
4. Users sync to IAM database on first login
```

### Method 3: Manual Creation via IAM API

Use the `test-workflow-users.http` file to verify users after creation.

## Verification

### Check Users in Database

```sql
SELECT u.email, u.first_name, u.last_name, r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE r.name IN ('ROLE_CHECKER', 'ROLE_CONFIRMER')
ORDER BY r.name, u.email;
```

**Expected Output:**
```
email                    | first_name | last_name | role_name
-------------------------+------------+-----------+------------------
admin.workflow@test.com  | Admin      | Workflow  | ROLE_CHECKER
checker1@test.com        | Alice      | Checker   | ROLE_CHECKER
checker2@test.com        | Bob        | Checker   | ROLE_CHECKER
admin.workflow@test.com  | Admin      | Workflow  | ROLE_CONFIRMER
confirmer1@test.com      | Charlie    | Confirmer | ROLE_CONFIRMER
confirmer2@test.com      | Diana      | Confirmer | ROLE_CONFIRMER
```

### Check via IAM API

```bash
# Get users with ROLE_CHECKER
curl http://localhost:8082/api/roles/ROLE_CHECKER/users

# Get users with ROLE_CONFIRMER
curl http://localhost:8082/api/roles/ROLE_CONFIRMER/users
```

## Usage in Workflow Tests

### Login as Checker

```bash
# Using Keycloak OAuth2
curl -X POST http://localhost:8180/realms/enterprise/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=enterprise-client" \
  -d "username=checker1@test.com" \
  -d "password=Checker123!"
```

### Login as Confirmer

```bash
curl -X POST http://localhost:8180/realms/enterprise/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=enterprise-client" \
  -d "username=confirmer1@test.com" \
  -d "password=Confirmer123!"
```

## Test Workflow Scenario

1. **Create Product** as any user
2. **Checker Approval** - Login as checker1 or checker2, approve product
3. **Confirmer Approval** - Login as confirmer1 or confirmer2, confirm product  
4. **Verify Status** - Product status should be ACTIVE

## Cleanup

To remove test users:

```sql
-- Delete test users
DELETE FROM user_roles WHERE user_id IN (
  SELECT id FROM users WHERE email LIKE '%@test.com'
);

DELETE FROM users WHERE email LIKE '%@test.com';
```

## Files

- `V7__Seed_Test_Users_For_Workflow.sql` - Flyway migration to create users in DB
- `create-keycloak-users.http` - Keycloak Admin API calls to create users
- `test-workflow-users.http` - API calls to verify users
- `TEST_USERS.md` - This documentation
