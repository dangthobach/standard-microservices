# Quick Start Guide

Hướng dẫn nhanh để chạy hệ thống microservices trong môi trường local.

## Prerequisites

Đảm bảo bạn đã cài đặt:

- [x] **Java 21** (Eclipse Temurin hoặc Oracle JDK)
  ```bash
  java -version
  # Phải là version 21.x.x
  ```

- [x] **Docker Desktop** 24+
  ```bash
  docker --version
  docker-compose --version
  ```

- [x] **Maven** 3.9+
  ```bash
  mvn -version
  ```

- [x] **Node.js** 20+ (nếu chạy frontend)
  ```bash
  node --version
  npm --version
  ```

## Bước 1: Build Project

```bash
# Build tất cả modules
mvn clean package -DskipTests

# Hoặc build từng service riêng lẻ
cd gateway-service && mvn clean package -DskipTests
cd ../iam-service && mvn clean package -DskipTests
cd ../business-service && mvn clean package -DskipTests
```

## Bước 2: Start Infrastructure

Khởi động các infrastructure components (databases, cache, messaging, observability):

```bash
docker-compose up -d \
  postgres-iam \
  postgres-business \
  postgres-process \
  redis \
  zookeeper \
  kafka \
  keycloak \
  zipkin \
  prometheus \
  grafana
```

Đợi khoảng 30-60 giây cho các services khởi động hoàn toàn.

### Kiểm tra health:

```bash
# Check tất cả containers đang chạy
docker-compose ps

# Xem logs
docker-compose logs -f keycloak
```

## Bước 3: Configure Keycloak

### 3.1. Access Keycloak Admin Console

Mở browser: http://localhost:8180

**Login credentials:**
- Username: `admin`
- Password: `admin`

### 3.2. Create Realm

1. Click dropdown "master" (góc trên bên trái)
2. Click "Create Realm"
3. Realm name: `enterprise`
4. Click "Create"

### 3.3. Create Client for Gateway

1. Click "Clients" → "Create client"
2. **General Settings:**
   - Client ID: `gateway-service`
   - Name: `Gateway Service`
   - Click "Next"

3. **Capability config:**
   - Client authentication: ON
   - Authorization: OFF
   - Standard flow: ON
   - Direct access grants: ON
   - Click "Next"

4. **Login settings:**
   - Valid redirect URIs: `http://localhost:8080/*`
   - Web origins: `http://localhost:8080`
   - Click "Save"

5. **Get Client Secret:**
   - Go to "Credentials" tab
   - Copy "Client Secret" (sẽ cần cho config sau)

### 3.4. Create Client for Frontend

1. Click "Clients" → "Create client"
2. **General Settings:**
   - Client ID: `enterprise-frontend`
   - Name: `Enterprise Frontend`
   - Click "Next"

3. **Capability config:**
   - Client authentication: OFF (public client)
   - Standard flow: ON
   - Direct access grants: ON
   - Click "Next"

4. **Login settings:**
   - Valid redirect URIs: `http://localhost:4200/*`
   - Valid post logout redirect URIs: `http://localhost:4200/*`
   - Web origins: `http://localhost:4200`
   - Click "Save"

5. **Advanced settings:**
   - Go to "Advanced" tab
   - Proof Key for Code Exchange Code Challenge Method: `S256`
   - Click "Save"

### 3.5. Create Test User

1. Click "Users" → "Create new user"
2. **User details:**
   - Username: `testuser`
   - Email: `testuser@enterprise.com`
   - First name: `Test`
   - Last name: `User`
   - Email verified: ON
   - Click "Create"

3. **Set Password:**
   - Go to "Credentials" tab
   - Click "Set password"
   - Password: `Test@123`
   - Temporary: OFF
   - Click "Save"

## Bước 4: Start Microservices

```bash
# Start Gateway Service
docker-compose up -d gateway-service

# Start IAM Service
docker-compose up -d iam-service

# Start Business Service
docker-compose up -d business-service

# (Optional) Start other services
docker-compose up -d process-service integration-service
```

### Monitor Logs

```bash
# Xem logs của Gateway
docker-compose logs -f gateway-service

# Xem logs của tất cả services
docker-compose logs -f gateway-service iam-service business-service
```

## Bước 5: Verify Deployment

### Check Health Endpoints

```bash
# Gateway Health
curl http://localhost:8080/actuator/health

# Expected:
# {"status":"UP"}

# IAM Service Health
curl http://localhost:8081/actuator/health

# Business Service Health
curl http://localhost:8082/actuator/health
```

### Check All Services

```bash
# Tất cả services đều phải có status "healthy"
docker-compose ps
```

Expected output:
```
NAME                    STATUS      PORTS
gateway-service         healthy     0.0.0.0:8080->8080/tcp
iam-service             healthy     0.0.0.0:8081->8081/tcp
business-service        healthy     0.0.0.0:8082->8082/tcp
keycloak                healthy     0.0.0.0:8180->8080/tcp
postgres-iam            healthy     0.0.0.0:5432->5432/tcp
redis-cluster           healthy     0.0.0.0:6379->6379/tcp
zipkin                  healthy     0.0.0.0:9411->9411/tcp
...
```

## Bước 6: Test API

### 6.1. Get Access Token

```bash
curl -X POST 'http://localhost:8180/realms/enterprise/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=enterprise-frontend' \
  -d 'username=testuser' \
  -d 'password=Test@123'
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer"
}
```

Copy `access_token` từ response.

### 6.2. Call Protected API

```bash
# Set token as variable
TOKEN="<paste_your_access_token_here>"

# Call IAM Service through Gateway
curl -X GET 'http://localhost:8080/api/iam/users' \
  -H "Authorization: Bearer $TOKEN"

# Call Business Service through Gateway
curl -X GET 'http://localhost:8080/api/business/orders' \
  -H "Authorization: Bearer $TOKEN"
```

## Bước 7: Access Web Interfaces

### Keycloak Admin Console
- URL: http://localhost:8180
- Username: `admin`
- Password: `admin`

### Zipkin (Distributed Tracing)
- URL: http://localhost:9411
- No authentication required
- View traces của tất cả API requests

### Prometheus (Metrics)
- URL: http://localhost:9090
- No authentication required
- Query metrics: `http_server_requests_seconds_count`

### Grafana (Dashboards)
- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

**Add Prometheus Data Source:**
1. Configuration → Data Sources → Add data source
2. Select "Prometheus"
3. URL: `http://prometheus:9090`
4. Click "Save & Test"

## Bước 8: Frontend (Optional)

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

Access: http://localhost:4200

**Login:**
- Click "Login"
- Redirect to Keycloak
- Username: `testuser`
- Password: `Test@123`

## Common Issues & Solutions

### Issue 1: Port Already in Use

```bash
# Kiểm tra port đang sử dụng
netstat -ano | findstr :8080
netstat -ano | findstr :5432

# Kill process (Windows)
taskkill /PID <PID> /F

# Kill process (Linux/Mac)
kill -9 <PID>
```

### Issue 2: Docker Out of Memory

```bash
# Increase Docker memory
# Docker Desktop → Settings → Resources → Memory: 8GB+
```

### Issue 3: Services Not Starting

```bash
# Remove all containers and volumes
docker-compose down -v

# Rebuild and start
docker-compose up -d --build
```

### Issue 4: Database Connection Error

```bash
# Check PostgreSQL logs
docker-compose logs postgres-iam

# Restart database
docker-compose restart postgres-iam
```

## Stopping Services

```bash
# Stop tất cả services
docker-compose down

# Stop và xóa volumes (DATABASE SẼ MẤT DATA!)
docker-compose down -v

# Stop một service cụ thể
docker-compose stop gateway-service
```

## Next Steps

1. **Read Documentation:**
   - [Architecture Overview](ARCHITECTURE.md)
   - [Deployment Guide](DEPLOYMENT.md)

2. **Explore Code:**
   - Gateway: [gateway-service/](gateway-service/)
   - IAM Service: [iam-service/](iam-service/)
   - Business Service: [business-service/](business-service/)

3. **Development:**
   - Add new features
   - Write tests
   - Deploy to Kubernetes

## Support

Issues? Contact:
- GitHub Issues: [repository-url]/issues
- Email: support@enterprise.com

## Checklist

- [ ] Java 21 installed
- [ ] Docker Desktop running
- [ ] Maven installed
- [ ] Project built successfully
- [ ] Infrastructure services started
- [ ] Keycloak configured
- [ ] Microservices started
- [ ] Health checks passing
- [ ] API calls successful
- [ ] Web interfaces accessible

Chúc bạn thành công!
