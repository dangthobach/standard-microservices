# API Call Workflow: Business Service

This document outlines the workflow for a client API call to the `business-service`, covering Authentication (AuthN), Authorization (AuthZ), and the routing process involving the API Gateway and IAM (Keycloak).

## Architecture Overview

*   **Pattern**: Backend for Frontend (BFF)
*   **Entry Point**: `gateway-service` (Spring Cloud Gateway)
*   **Identity Provider**: Keycloak
*   **Session Management**: Gateway manages sessions; Frontend uses `SESSION_ID` cookie.
*   **Resource Server**: `business-service` validates JWT tokens.

## Workflow Diagram

```mermaid
sequenceDiagram
    participant User as Client (Browser)
    participant GW as Gateway Service
    participant Redis as Redis (Session Store)
    participant KC as Keycloak (IAM)
    participant BS as Business Service

    Note over User, GW: 1. Initial Request (Authenticated)

    User->>GW: GET /api/business/v1/products<br/>Cookie: SESSION_ID=12345
    
    activate GW
    Note right of GW: JwtEnrichmentFilter
    GW->>Redis: GET session:12345
    Redis-->>GW: { "accessToken": "ey...", "userId": "user1" }
    
    Note right of GW: Token Validation & Enrichment
    alt Token Expired
        GW->>KC: POST /token (refresh_token)
        KC-->>GW: New Access Token
        GW->>Redis: Update session
    end

    GW->>GW: Add Header: Authorization: Bearer ey...
    GW->>Redis: SET online:user1 (Heartbeat)
    
    Note right of GW: Routing
    GW->>BS: GET /api/business/v1/products<br/>Authorization: Bearer ey...
    
    activate BS
    Note right of BS: Security Filter Chain
    BS->>BS: Validate JWT (Signature, Exp, Issuer)
    BS->>BS: Check Roles (AuthZ)
    
    BS->>BS: Execute Logic (Controller -> Service -> DB)
    
    BS-->>GW: 200 OK (JSON Data)
    deactivate BS
    
    GW-->>User: 200 OK (JSON Data)
    deactivate GW
```

## Step-by-Step Flow

### 1. Client Request
The user sends a request to the Gateway. Since this is a BFF architecture, the client (Angular/React) sends a `SESSION_ID` cookie instead of a raw JWT.

**Request:**
```http
GET /api/business/v1/products HTTP/1.1
Host: api.enterprise.com
Cookie: SESSION_ID=a1b2c3d4-e5f6-7890-1234-567890abcdef
Accept: application/json
```

---

### 2. Gateway Processing (Auth & Enrichment)
The `gateway-service` intercepts the request using `JwtEnrichmentFilter`.

1.  **Session Lookup**: The Gateway extracts `SESSION_ID` and queries Redis (L2 Cache) or Local Caffeine Cache (L1).
2.  **Token Retrieval**: It retrieves the associated `access_token`.
3.  **Token Refresh (Optional)**: If the token is expired, the Gateway uses the stored `refresh_token` to get a new one from Keycloak implicitly.
4.  **Header Injection**: The Gateway injects the `Authorization` header.

**Enriched Request (Internal to Cluster):**
```http
GET /v1/products HTTP/1.1
Host: business-service:8082
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE...
X-Forwarded-For: 203.0.113.195
X-Trace-Id: 9876543210
```

---

### 3. Business Service (Resource Server)
The `business-service` receives the request. It acts as an OAuth2 Resource Server.

1.  **JWT Validation**: Spring Security validates the JWT signature using the Public Key (retrieved from Keycloak JWKS).
    *   Checks `iss` (Issuer): `http://keycloak:8080/realms/enterprise`
    *   Checks `exp` (Expiration).
2.  **Authorization (AuthZ)**: Checks if the user has the required roles (e.g., `ROLE_USER` or `SCOPE_read`).

**Controller Logic:**
```java
@RestController
@RequestMapping("/v1/products")
public class ProductController {
    
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public ResponseEntity<List<Product>> getProducts() {
        // Business logic...
        return ResponseEntity.ok(products);
    }
}
```

---

### 4. Response
The service processes the request and returns the data.

**Response (Internal):**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "101",
    "name": "Enterprise Subscription",
    "price": 999.00
  }
]
```

The Gateway forwards this response back to the client.
