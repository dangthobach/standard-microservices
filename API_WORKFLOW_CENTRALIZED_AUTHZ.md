# API Workflow: Centralized Authorization (Gateway AuthZ)

This workflow is optimized for **1 Million CCU** by centralizing Authorization checks at the Gateway using Multi-Level Caching (Redis + Caffeine), reducing load on downstream services.

## Architecture Diagram

```mermaid
sequenceDiagram
    participant User as Client
    participant GW as Gateway Service
    participant Cache as L1/L2 Cache (Redis/Caffeine)
    participant IAM as IAM Service
    participant BS as Business Service

    Note over User, GW: 1. Request
    User->>GW: GET /api/business/v1/resource<br/>Cookie: SESSION_ID=xyz

    activate GW
    Note right of GW: JwtEnrichmentFilter
    GW->>Cache: [Session] Get Access Token
    Cache-->>GW: Token Found

    Note right of GW: CentralizedAuthZFilter
    GW->>GW: Determine Required Perms<br/>(e.g., "business:read")
    
    GW->>Cache: [AuthZ] Get Permissions for User
    alt Cache Miss
        Cache-->>GW: Null
        GW->>IAM: GET /api/auth/permissions/{userId}
        IAM-->>GW: ["business:read", "user:write"]
        GW->>Cache: SET authz:perm:{userId} (TTL 1h)
    else Cache Hit
        Cache-->>GW: ["business:read", ...]
    end

    GW->>GW: Check if "business:read" in permissions

    alt Access Denied
        GW-->>User: 403 Forbidden
    else Access Granted
        Note right of GW: Routing
        GW->>BS: GET /v1/resource<br/>Auth: Bearer {token}<br/>X-Permissions: business:read
        
        activate BS
        Note right of BS: Business Logic
        BS->>BS: (Optional) Validate Token Signature
        BS-->>GW: 200 OK
        deactivate BS
        
        GW-->>User: 200 OK
    end
    deactivate GW
```

## Critical Performance Components

### 1. Multi-Level Caching
To handle 1M users, we cannot query IAM for every HTTP request.
*   **L1 Cache (Caffeine - Heap)**: Stores permissions for active users. TTL ~60s. Access: **Microseconds**.
*   **L2 Cache (Redis - Distributed)**: Stores permissions. TTL ~1h. Access: **Milliseconds**.

### 2. Authorization Logic
*   **Map-based Matching**: Gateway holds a config map of `Path Pattern -> Permission`.
    *   `/api/business/v1/products/**` (GET) -> `business:product:read`
    *   `/api/business/v1/orders/**` (POST) -> `business:order:create`

### 3. Failover
*   If Redis is down, Gateway *can* fall back to calling IAM directly (with circuit breaker) or deny access defensively.

## Repo Review Status

Use of this workflow requires code changes:
*   [ ] **Gateway**: Need `AuthZService` and `CentralizedAuthZFilter`.
*   [ ] **IAM**: Need `/api/auth/permissions` endpoint.
*   [ ] **Business**: Can simplify security config (trust Gateway or keep simple JWT check).

This implementation is **feasibly scalable** to 1M CCU if caching is tuned correctly.
