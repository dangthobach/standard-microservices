# Quick Start - OAuth2 PKCE Authentication

## 🚀 Quick Start (5 minutes)

### Prerequisites

- Docker & Docker Compose
- Java 21+
- Maven 3.9+
- Node.js 18+ & npm
- Git

### Step 1: Start Infrastructure

```bash
# Start Keycloak, Redis, Postgres, Zipkin
docker-compose up -d keycloak redis postgres-iam zipkin

# Wait for Keycloak to be ready (may take 1-2 minutes)
docker-compose logs -f keycloak
# Look for: "Keycloak 26.0 started"
```

### Step 2: Configure Keycloak

**Option A: Manual Setup** (Recommended for learning):

1. Access Keycloak Admin Console:
   ```
   URL: http://localhost:8180/admin
   Username: admin
   Password: admin
   ```

2. Create Realm "enterprise":
   - Click "Create Realm"
   - Name: `enterprise`
   - Enabled: Yes
   - Click "Create"

3. Create Client "enterprise-frontend":
   - Clients → Create Client
   - Client ID: `enterprise-frontend`
   - Client Protocol: `openid-connect`
   - Next →
   - Client authentication: OFF (Public client)
   - Authorization: OFF
   - Authentication flow:
     - ☑ Standard flow
     - ☐ Direct access grants
     - ☐ Implicit flow
   - Next →
   - Valid redirect URIs:
     - `http://localhost:4200/*`
     - `http://localhost:4200/auth/callback`
   - Valid post logout redirect URIs:
     - `http://localhost:4200`
   - Web origins:
     - `http://localhost:4200`
   - Save →
   - Go to "Advanced" tab
   - Proof Key for Code Exchange Code Challenge Method: `S256`
   - Save

4. Create Client "gateway-service":
   - Clients → Create Client
   - Client ID: `gateway-service`
   - Next →
   - Client authentication: ON (Confidential)
   - Next →
   - Valid redirect URIs:
     - `http://localhost:8080/*`
     - `http://localhost:8080/login/oauth2/code/keycloak`
   - Save →
   - Go to "Credentials" tab
   - Copy the "Client Secret" (you'll need this)

5. Create Roles:
   - Realm roles → Create role
   - Role name: `USER` → Save
   - Create role: `ADMIN` → Save
   - Create role: `MANAGER` → Save

6. Create Test User:
   - Users → Create new user
   - Username: `testuser`
   - Email: `testuser@example.com`
   - First name: `Test`
   - Last name: `User`
   - Email verified: ON
   - Create →
   - Go to "Credentials" tab
   - Set password: `testuser123`
   - Temporary: OFF
   - Set Password →
   - Go to "Role mapping" tab
   - Assign roles → Select "USER" → Assign

**Option B: Automated Setup** (Quick):

```bash
# Windows
cd infrastructure\keycloak
setup-keycloak.bat

# Linux/Mac
cd infrastructure/keycloak
chmod +x setup-keycloak.sh
./setup-keycloak.sh
```

### Step 3: Configure Gateway

Set the Keycloak client secret:

**Windows:**
```cmd
set KEYCLOAK_CLIENT_SECRET=<your-client-secret-from-step-2.4>
```

**Linux/Mac:**
```bash
export KEYCLOAK_CLIENT_SECRET=<your-client-secret-from-step-2.4>
```

### Step 4: Start Backend

```bash
# Build all services
mvn clean install -DskipTests

# Start Gateway
cd gateway-service
mvn spring-boot:run
```

Gateway will start on: http://localhost:8080

Verify health:
```bash
curl http://localhost:8080/actuator/health
```

### Step 5: Start Frontend

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm start
```

Frontend will start on: http://localhost:4200

### Step 6: Test Authentication Flow

1. **Navigate to Frontend**:
   ```
   http://localhost:4200
   ```

2. **You'll be redirected to Login page**

3. **Click "Login with Keycloak"**

4. **Enter Credentials**:
   - Username: `testuser`
   - Password: `testuser123`

5. **You'll be redirected to Dashboard**

6. **Verify Authentication**:
   - Check username in top-right corner
   - Click "Test API" button to verify backend communication
   - Check browser console for OAuth2 events
   - Check localStorage for SESSION_ID

---

## 🔍 Verification Steps

### 1. Check Keycloak

```bash
# Verify Keycloak is running
curl http://localhost:8180/health/ready

# Should return: {"status":"UP"}
```

### 2. Check Gateway

```bash
# Health check
curl http://localhost:8080/actuator/health

# Test public endpoint
curl http://localhost:8080/auth/login

# Test protected endpoint (should return 401)
curl http://localhost:8080/api/users
```

### 3. Check Redis (Sessions)

```bash
# Connect to Redis
docker exec -it enterprise-redis redis-cli

# List session keys
KEYS session:*

# Get session details
GET session:<session-id>

# Check TTL
TTL session:<session-id>
```

### 4. Check Browser

**LocalStorage**:
- Open DevTools → Application → Local Storage
- Should see: `SESSION_ID`, `redirect_url`

**Cookies**:
- Open DevTools → Application → Cookies
- Should see: `SESSION_ID` (HttpOnly)

**Network**:
- Open DevTools → Network
- Login flow should show:
  1. Redirect to Keycloak
  2. POST to `/token` endpoint
  3. POST to `/auth/session`
  4. Redirect to `/dashboard`

---

## 🧪 Test Scenarios

### Test 1: Login Flow

```
1. Navigate to http://localhost:4200
2. Click "Login with Keycloak"
3. Enter: testuser / testuser123
4. ✓ Should redirect to dashboard
5. ✓ Should see "Welcome, testuser!"
6. ✓ Should see SESSION_ID in localStorage
```

### Test 2: Protected Route

```
1. Logout
2. Try to navigate to http://localhost:4200/users
3. ✓ Should redirect to login
4. ✓ After login, should redirect back to /users
```

### Test 3: API Call

```
1. Login
2. On dashboard, click "Test API" button
3. ✓ Should see API response with user info
4. ✓ Check Network tab - request should have:
   - Authorization: Bearer <token>
   - X-Session-Id: <session-id>
   - Cookie: SESSION_ID=<session-id>
```

### Test 4: Session Persistence

```
1. Login
2. Refresh page (F5)
3. ✓ Should remain logged in
4. ✓ Should not redirect to login
5. ✓ Check Redis - session should still exist
```

### Test 5: Logout

```
1. Login
2. Click user menu → Logout
3. ✓ Should redirect to login page
4. ✓ SESSION_ID should be removed from localStorage
5. ✓ Session should be deleted from Redis
6. ✓ Should be logged out from Keycloak
```

---

## 🐛 Troubleshooting

### Issue: "Keycloak not ready"

**Solution**:
```bash
# Check Keycloak logs
docker-compose logs keycloak

# Restart Keycloak
docker-compose restart keycloak

# Wait for "Keycloak started" message
```

### Issue: "Invalid redirect_uri"

**Solution**:
- Verify redirect URIs in Keycloak client config
- Should include: `http://localhost:4200/*` and `http://localhost:4200/auth/callback`

### Issue: "PKCE validation failed"

**Solution**:
- Ensure client "enterprise-frontend" has:
  - Advanced → Proof Key for Code Exchange Code Challenge Method: `S256`

### Issue: "SESSION_ID not created"

**Solution**:
```bash
# Check Gateway logs
cd gateway-service
mvn spring-boot:run

# Look for:
# - "Session created: <session-id>"
# - Any errors from SessionService

# Check Redis connection
docker exec -it enterprise-redis redis-cli ping
# Should return: PONG
```

### Issue: "401 Unauthorized on API calls"

**Solution**:
1. Check if SESSION_ID exists in localStorage
2. Check if SESSION_ID cookie is sent (DevTools → Network)
3. Verify session exists in Redis:
   ```bash
   docker exec -it enterprise-redis redis-cli
   KEYS session:*
   GET session:<your-session-id>
   ```
4. Check Gateway logs for JWT validation errors

### Issue: "CORS error"

**Solution**:
- Verify Gateway CORS configuration allows `http://localhost:4200`
- Check browser console for specific CORS error
- Ensure `withCredentials: true` in HTTP requests

---

## 📊 Default Credentials

### Keycloak Admin Console
- URL: http://localhost:8180/admin
- Username: `admin`
- Password: `admin`

### Test Users
- **Regular User**:
  - Username: `testuser`
  - Password: `testuser123`
  - Roles: USER

- **Admin User** (if using automated setup):
  - Username: `admin`
  - Password: `admin123`
  - Roles: ADMIN, USER

### Clients
- **Frontend (Public)**:
  - Client ID: `enterprise-frontend`
  - PKCE: S256 required
  - Redirect URIs: http://localhost:4200/*

- **Gateway (Confidential)**:
  - Client ID: `gateway-service`
  - Client Secret: Set via environment variable
  - Redirect URIs: http://localhost:8080/*

---

## 🔗 Useful URLs

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:4200 | Angular SPA |
| **Gateway** | http://localhost:8080 | API Gateway |
| **Keycloak** | http://localhost:8180 | SSO / Admin Console |
| **Keycloak Realms** | http://localhost:8180/realms/enterprise | OpenID Configuration |
| **Gateway Health** | http://localhost:8080/actuator/health | Health check |
| **Gateway Auth** | http://localhost:8080/auth/me | Current user info |
| **Redis** | localhost:6379 | Session storage |
| **Zipkin** | http://localhost:9411 | Distributed tracing |

---

## 🎯 Next Steps

After completing Quick Start:

1. **Explore Features**:
   - Try different routes (/users, /organizations)
   - Test logout and re-login
   - Inspect JWT tokens (jwt.io)
   - Check distributed tracing in Zipkin

2. **Customize**:
   - Add your own protected routes
   - Customize login page styling
   - Add role-based authorization
   - Implement profile page

3. **Production Setup**:
   - Use HTTPS (SSL certificates)
   - Secure CORS (specific origins)
   - Change default passwords
   - Use secrets management (Vault)
   - Configure production Keycloak realm

4. **Advanced**:
   - Implement token refresh
   - Add MFA (Multi-Factor Authentication)
   - Configure session timeout
   - Add user registration flow
   - Implement forgot password

---

## 📚 Documentation

- [OAUTH2_PKCE_ANALYSIS.md](OAUTH2_PKCE_ANALYSIS.md) - Gap analysis
- [OAUTH2_PKCE_IMPLEMENTATION_COMPLETE.md](OAUTH2_PKCE_IMPLEMENTATION_COMPLETE.md) - Full implementation guide
- [ENTITY_FRAMEWORK_IMPROVEMENTS.md](ENTITY_FRAMEWORK_IMPROVEMENTS.md) - JPA Auditing & CQRS

---

**Status**: ✅ Ready for Testing
**Support**: For issues, check troubleshooting section or GitHub issues
