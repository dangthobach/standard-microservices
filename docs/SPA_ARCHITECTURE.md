# SPA Architecture Documentation

## Overview

Hệ thống sử dụng **2 Single Page Applications (SPA)** riêng biệt, mỗi ứng dụng phục vụ một nhóm người dùng khác nhau với vai trò và chức năng rõ ràng.

---

## Architecture Diagram

```mermaid
graph TB
    subgraph "Users"
        BU[Business Users<br/>Product Managers, Approvers]
        AD[IT/Ops Admins<br/>Process Admins, DevOps]
    end
    
    subgraph "Frontend Layer"
        EF[Enterprise Frontend<br/>Angular 18 - Port 80]
        FA[Admin UI<br/>React - Port 3001]
    end
    
    subgraph "Gateway"
        GW[API Gateway<br/>Spring Cloud Gateway - Port 8080]
    end
    
    subgraph "Backend Services"
        IAM[iam-service]
        BIZ[business-service]
        PMS[process-management-service]
        INT[integration-service]
    end
    
    BU --> EF
    AD --> FA
    
    EF --> GW
    FA --> GW
    
    GW --> IAM
    GW --> BIZ
    GW --> PMS
    GW --> INT
```

---

## SPA 1: Enterprise Frontend

| Attribute | Value |
|-----------|-------|
| **Technology** | Angular 18 + Material Design |
| **Port** | 80 (Production) / 4200 (Dev) |
| **Location** | `frontend/` |
| **Target Users** | Business Users (Product Managers, Approvers) |
| **Auth** | Keycloak OIDC |

### Features

| Module | Route | Description |
|--------|-------|-------------|
| Dashboard | `/dashboard` | Business KPIs, pending tasks summary |
| Products | `/products` | Product CRUD, submit for approval |
| My Tasks | `/workflow/tasks` | Tasks assigned to current user |
| Customers | `/customers` | Customer management |
| Organizations | `/organizations` | Organization management |
| Users | `/users` | User management |
| Settings | `/settings` | User preferences |

### Key Interactions

```mermaid
sequenceDiagram
    actor User as Business User
    participant EF as Enterprise Frontend
    participant GW as Gateway
    participant BIZ as business-service
    participant PMS as process-management-service
    
    User->>EF: Create Product
    EF->>GW: POST /api/business/products
    GW->>BIZ: Forward request
    BIZ->>PMS: Start approval workflow
    PMS-->>BIZ: Process started
    BIZ-->>GW: Product created
    GW-->>EF: Response
    EF-->>User: Product pending approval

    User->>EF: Complete Task
    EF->>GW: POST /api/process/flow/tasks/{id}/complete
    GW->>PMS: Forward request
    PMS-->>GW: Task completed
    GW-->>EF: Response
    EF-->>User: Task completed
```

---

## SPA 2: Admin UI

| Attribute | Value |
|-----------|-------|
| **Technology** | React 18 + Ant Design + TailwindCSS |
| **Port** | 3001 (Production) / 3000 (Dev) |
| **Location** | `admin-ui/` |
| **Target Users** | IT/Operations Admins |
| **Auth** | (Future: Keycloak) |

### Features

| Module | Route | Description |
|--------|-------|-------------|
| Operations Dashboard | `/process-management/dashboard` | Process metrics, system health |
| Process Monitor | `/process-management/monitor` | Real-time process tracking |
| Deployment Center | `/process-management/deployments` | BPMN deployment management |
| Version Diff | `/deployments/:id/diff` | Compare BPMN versions |
| DMN Management | `/decisions` | Decision table configuration |
| Analytics Dashboard | `/analytics/dashboard` | Deep workflow analytics |
| Service Catalog | `/integration/catalog` | API integrations |

### Key Interactions

```mermaid
sequenceDiagram
    actor Admin as IT Admin
    participant FA as Flowable Admin UI
    participant GW as Gateway
    participant PMS as process-management-service
    
    Admin->>FA: Deploy new BPMN
    FA->>GW: POST /api/process/deployments
    GW->>PMS: Forward request
    PMS-->>GW: Deployment successful
    GW-->>FA: Response
    FA-->>Admin: BPMN deployed

    Admin->>FA: Monitor stuck process
    FA->>GW: GET /api/process/flow/processes/{id}
    GW->>PMS: Forward request
    PMS-->>GW: Process details
    GW-->>FA: Response
    FA-->>Admin: Show process state & history
```

---

## Role Separation Summary

| Capability | Enterprise Frontend | Flowable Admin UI |
|------------|---------------------|-------------------|
| View own tasks | ✅ | ❌ |
| Approve/Reject products | ✅ | ❌ |
| Create products | ✅ | ❌ |
| Deploy BPMN | ❌ | ✅ |
| Monitor all processes | ❌ | ✅ |
| Configure DMN | ❌ | ✅ |
| Deep analytics | ❌ | ✅ |
| Troubleshoot workflows | ❌ | ✅ |
| Manage users | ✅ | ❌ |

---

## Access Control

```mermaid
graph LR
    subgraph "Keycloak Roles"
        R1[BUSINESS_USER]
        R2[PRODUCT_MANAGER]
        R3[APPROVER]
        R4[PROCESS_ADMIN]
        R5[IT_ADMIN]
    end
    
    subgraph "Enterprise Frontend"
        EF1[Dashboard]
        EF2[Products]
        EF3[My Tasks]
    end
    
    subgraph "Flowable Admin UI"
        FA1[Deployments]
        FA2[Monitor]
        FA3[Analytics]
    end
    
    R1 --> EF1
    R2 --> EF2
    R3 --> EF3
    R4 --> FA1
    R4 --> FA2
    R5 --> FA3
```

---

## Deployment

| Environment | Enterprise Frontend | Flowable Admin UI |
|-------------|---------------------|-------------------|
| Development | http://localhost:4200 | http://localhost:3000 |
| Production | https://app.enterprise.com | https://admin.enterprise.com |

Both SPAs communicate with backend through the same **API Gateway** on port 8080.

---

## API Endpoints Usage

### Enterprise Frontend consumes:
```
/api/business/*          → business-service
/api/iam/*               → iam-service
/api/process/flow/tasks  → process-management-service (user's tasks only)
```

### Flowable Admin UI consumes:
```
/api/process/flow/*       → All process management
/api/process/analytics/*  → All analytics
/api/process/deployments  → Deployment management
/api/process/decisions/*  → DMN management
```
