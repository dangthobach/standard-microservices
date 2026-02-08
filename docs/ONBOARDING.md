# Developer Onboarding Guide

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd standard-microservices

# Build backend
mvn clean package -DskipTests

# Build frontends
cd frontend && npm install && npm run build && cd ..
cd admin-ui && npm install && npm run build && cd ..
```

### 2. Start Infrastructure

```bash
docker-compose up -d postgres-iam postgres-business redis kafka keycloak
```

### 3. Start Services

```bash
docker-compose up -d gateway-service iam-service business-service process-management-service
```

### 4. Start Frontends

```bash
# Terminal 1 - Enterprise Frontend
cd frontend && npm start  # http://localhost:4200

# Terminal 2 - Admin UI
cd admin-ui && npm start  # http://localhost:3000
```

---

## Architecture Overview

| Component | Technology | Port |
|-----------|------------|------|
| API Gateway | Spring Cloud Gateway | 8080 |
| IAM Service | Spring Boot + Virtual Threads | 8081 |
| Business Service | Spring Boot + Flowable | 8082 |
| Process Management | Spring Boot + Flowable | 8083 |
| Enterprise Frontend | Angular 18 | 4200/80 |
| Admin UI | React + Ant Design | 3000/3001 |
| Keycloak | - | 8180 |
| PostgreSQL | - | 5432, 5433 |
| Redis | - | 6379 |

---

## Key Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/flow/tasks/my` | Get current user's tasks |
| `POST /api/flow/tasks/{id}/delegate` | Delegate task |
| `POST /api/flow/tasks/{id}/complete` | Complete task |
| `GET /api/analytics/workflow/statistics` | Analytics overview |

---

## Documentation

- [SPA Architecture](docs/SPA_ARCHITECTURE.md) - Frontend architecture
- [Deployment Guide](DEPLOYMENT_GUIDE.md) - Production deployment
- [Workflow Features](docs/WORKFLOW_ADVANCED_FEATURES.md) - Workflow documentation
