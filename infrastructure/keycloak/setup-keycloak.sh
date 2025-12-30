#!/bin/bash

# Keycloak Setup Script
#
# This script:
# 1. Waits for Keycloak to be ready
# 2. Imports the enterprise realm
# 3. Creates test users
# 4. Configures clients for PKCE

set -e

KEYCLOAK_URL="http://localhost:8180"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM_FILE="./enterprise-realm.json"

echo "==============================================="
echo "Keycloak Setup Script"
echo "==============================================="

# Wait for Keycloak to be ready
echo ""
echo "[1/4] Waiting for Keycloak to be ready..."
until $(curl --output /dev/null --silent --head --fail ${KEYCLOAK_URL}); do
    printf '.'
    sleep 5
done
echo " ✓ Keycloak is ready!"

# Get admin access token
echo ""
echo "[2/4] Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | jq -r '.access_token')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo " ✗ Failed to get admin token"
    exit 1
fi
echo " ✓ Admin token obtained"

# Check if realm already exists
echo ""
echo "[3/4] Checking if 'enterprise' realm exists..."
REALM_EXISTS=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/enterprise" \
  -w "%{http_code}" -o /dev/null)

if [ "$REALM_EXISTS" == "200" ]; then
    echo " ⚠ Realm 'enterprise' already exists"
    echo " Would you like to delete and recreate it? (y/n)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo " Deleting existing realm..."
        curl -s -X DELETE -H "Authorization: Bearer ${ADMIN_TOKEN}" \
          "${KEYCLOAK_URL}/admin/realms/enterprise"
        echo " ✓ Realm deleted"
    else
        echo " ✓ Skipping realm creation"
        exit 0
    fi
fi

# Import realm
echo ""
echo "[4/4] Importing 'enterprise' realm..."
if [ ! -f "$REALM_FILE" ]; then
    echo " ✗ Realm file not found: $REALM_FILE"
    exit 1
fi

curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d @${REALM_FILE}

echo " ✓ Realm imported successfully!"

# Verify setup
echo ""
echo "==============================================="
echo "Keycloak Setup Complete!"
echo "==============================================="
echo ""
echo "Keycloak URL: ${KEYCLOAK_URL}"
echo "Admin Console: ${KEYCLOAK_URL}/admin"
echo "Admin Username: ${ADMIN_USER}"
echo "Admin Password: ${ADMIN_PASSWORD}"
echo ""
echo "Realm: enterprise"
echo ""
echo "Test Users:"
echo "  - testuser / testuser123 (Role: USER)"
echo "  - admin / admin123 (Role: ADMIN, USER)"
echo ""
echo "Clients:"
echo "  - enterprise-frontend (Public, PKCE S256)"
echo "  - gateway-service (Confidential)"
echo ""
echo "Next steps:"
echo "  1. Start Gateway: cd gateway-service && mvn spring-boot:run"
echo "  2. Start Frontend: cd frontend && npm start"
echo "  3. Navigate to: http://localhost:4200"
echo ""
