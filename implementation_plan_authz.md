# Implementation Plan: Dynamic Centralized Authorization (AuthZ)

## Goal
Implement a **Dynamic, Database-Driven Centralized Authorization** mechanism. The API Gateway will enforce permissions based on **dynamic rules** stored in the IAM Database (instead of static YAML), supporting **1 Million CCU** via aggressive caching.

## Architecture Change
*   **Database-Driven Rules**: Protected routes and Public routes are defined in the IAM Database (`endpoint_protections` table).
*   **Gateway Synchronization**: Gateway loads these rules into memory at startup and refreshes them via Webhook/Event when changed.
*   **Centralized Enforcement**: Gateway matches request path -> looks up rule -> checks user permission OR allows if public.

## Detailed Implementation Logic

### 1. IAM Service (`iam-service`)

We need a new entity to map API Endpoints to Rules.

#### [NEW] Entity: `EndpointProtection`
Maps a concrete API route to a required permission OR marks it as public.

*   `id` (UUID)
*   `method` (String): `GET`, `POST`, `PUT`, `DELETE`, `*`
*   `pattern` (String): Ant-style path pattern (e.g., `/api/business/orders/**`)
*   `permissionCode` (String): The required permission (e.g., `ORDER:CREATE`). Nullable if `isPublic` is true.
*   `isPublic` (Boolean): If true, authentication/authorization is skipped (PermitAll).
*   `priority` (Int): To handle overlapping paths (higher priority wins)
*   `active` (Boolean): Enable/Disable rule

#### [NEW] API: Internal Policy Endpoint
**Endpoint**: `GET /api/internal/policies`
**Access**: Internal Only (Gateway)
**Description**: Returns list of active endpoint protections sorted by priority.

**Response**:
```json
[
  {
    "pattern": "/api/public/configs/**",
    "method": "GET",
    "isPublic": true,
    "priority": 100
  },
  {
    "pattern": "/api/business/orders/**",
    "method": "POST",
    "permission": "ORDER:CREATE",
    "isPublic": false,
    "priority": 10
  }
]
```

### 2. Gateway Service (`gateway-service`)

#### Logic: Policy Management
*   **Startup**: `PolicyManager` calls `GET /api/internal/policies` and builds a matching tree/list in memory.
*   **Caching**: `RoutePolicies` are cached in-memory (Heap).
*   **Refresh**: `POST /actuator/authz/refresh-policies` triggers a reload from IAM.

#### Logic: `CentralizedAuthZFilter`
1.  **Identify Policy**:
    *   Iterate through cached `RoutePolicies`.
    *   Match `request.path` and `request.method` using `AntPathMatcher`.
    *   **Result**: Found Policy (or null).
2.  **Enforce Policy**:
    *   **Case 1: No Policy Matches**: Default behavior (typically Deny, or Fallback to hardcoded safe list). *Measurement: Log warning.*
    *   **Case 2: `isPublic == true`**: **ALLOW** request immediately. (Skip JWT check if needed, or just skip Perm check).
    *   **Case 3: `permissionCode` is set**:
        *   Check User Permissions (L1 -> L2 -> IAM).
        *   If User has Permission -> **ALLOW**.
        *   Else -> **403 Forbidden**.

## Implementation Steps

### Step 1: IAM Service Changes
1.  Create `EndpointProtection` Entity with `isPublic` flag.
2.  Create Repository & Service.
3.  Create `InternalPolicyController`.
4.  Add initial data (include some public paths like `/api/configs`).

### Step 2: Gateway Service Changes
1.  Create `PolicyManager` Service.
2.  Update `JwtEnrichmentFilter` (optional) to respect new Public rules, OR let `CentralizedAuthZFilter` handle it.
    *   *Better approach*: `CentralizedAuthZFilter` runs *after* JWT extraction. if Public, it passes.
3.  Implement `CentralizedAuthZFilter`.

## API Specification (Internal)

#### Get All Policies
```http
GET /api/internal/policies HTTP/1.1
Host: iam-service
X-Internal-Secret: <secret>
```

#### Get User Permissions
```http
GET /api/internal/permissions/user/{userId} HTTP/1.1
Host: iam-service
X-Internal-Secret: <secret>
```

## Performance Considerations
*   **Public Endpoints**: Matched in-memory (nano-seconds) -> No Redis call needed -> Extremely fast.
*   **Scalability**: Validates 1M CCU requirements by bypassing expensive checks for public traffic.
