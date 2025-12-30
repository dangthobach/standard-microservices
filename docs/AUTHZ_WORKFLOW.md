# High-Performance Authorization Workflow (1M CCU) - BFF Pattern

This document outlines the Backend For Frontend (BFF) architecture where the Gateway manages security tokens, exposing only a secure Session to the Client.

## Table of Contents
1. [Core Architecture (BFF Pattern)](#1-core-architecture-bff-pattern)
2. [Authentication Flow (BFF)](#2-authentication-flow-bff)
3. [Risk Analysis & Solutions](#3-risk-analysis--solutions)
4. [API Specification (BFF Layer)](#4-api-specification-bff-layer)
5. [Gateway Implementation Logic](#5-gateway-implementation-logic)
6. [Performance Optimization for 1M CCU](#6-performance-optimization-for-1m-ccu)
7. [Security Considerations](#7-security-considerations)

---

## 1. Core Architecture (BFF Pattern)

### Components

| Component | Role | Technology |
|-----------|------|------------|
| **Client (Angular)** | "Dumb" Security. Holds only a `SESSION_ID` cookie. No access to JWTs. | Angular 17+ |
| **Gateway (BFF)** | Confidential Client. Exchanges Auth Code for Tokens. Maps `SESSION_ID` → `{Access, Refresh, Profile}` in Redis. Attaches `Authorization: Bearer token` to downstream requests. Handles Token Refresh automatically. | Spring Cloud Gateway (WebFlux) |
| **IdP** | Identity Provider (OIDC) | Keycloak |
| **Store** | Session Store | Redis (L2) + Caffeine (L1) |
| **Microservices** | Business logic services | Spring Boot (Virtual Threads) |

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Client (Angular)                                 │
│  - Holds: SESSION_ID cookie (HttpOnly, Secure, SameSite=Strict)        │
│  - Does NOT have: Access Token, Refresh Token                          │
└─────────────────────────────────────────────────────────────────────────┘
                                   ↓ ↑
                    Cookie: SESSION_ID=xyz123
                                   ↓ ↑
┌─────────────────────────────────────────────────────────────────────────┐
│                     Gateway (BFF - Confidential Client)                 │
│  ┌──────────────────────────────────────────────────────────────┐      │
│  │ L1 Cache (Caffeine)                                           │      │
│  │ SESSION_ID → Access Token (TTL: 60s)                          │      │
│  └──────────────────────────────────────────────────────────────┘      │
│                             ↓ (Cache Miss)                              │
│  ┌──────────────────────────────────────────────────────────────┐      │
│  │ L2 Store (Redis)                                              │      │
│  │ SESSION:{id} → {access_token, refresh_token, expires_at}      │      │
│  └──────────────────────────────────────────────────────────────┘      │
│                                                                          │
│  Security Filters:                                                      │
│  1. CSRF Validation (X-XSRF-TOKEN header for mutating requests)        │
│  2. Session Lookup (L1 → L2)                                           │
│  3. Token Refresh (if expired)                                         │
│  4. JWT Enrichment (Add Authorization header to downstream)            │
└─────────────────────────────────────────────────────────────────────────┘
                                   ↓ ↑
                 Authorization: Bearer eyJhbGc...
                                   ↓ ↑
┌─────────────────────────────────────────────────────────────────────────┐
│                      Microservices (IAM, Business)                      │
│  - Validate JWT signature                                              │
│  - Extract claims (sub, roles, etc.)                                   │
│  - Execute business logic                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Authentication Flow (BFF)

### Phase 1: Login (BFF Handshake)

```
┌────────┐       ┌────────┐       ┌───────┐       ┌──────────┐       ┌──────────┐
│ Client │       │Gateway │       │ Redis │       │ Keycloak │       │Microserv.│
│(Angular)       │ (BFF)  │       │ (L2)  │       │  (IdP)   │       │          │
└────┬───┘       └───┬────┘       └───┬───┘       └────┬─────┘       └────┬─────┘
     │               │                │                │                  │
     │1. GET /auth/login               │                │                  │
     ├──────────────>│                │                │                  │
     │               │                │                │                  │
     │2. 302 Redirect to Keycloak (PKCE: code_challenge)                  │
     │<──────────────┤                │                │                  │
     │               │                │                │                  │
     │3. User Login (username + password)                                 │
     ├───────────────────────────────────────────────>│                  │
     │               │                │                │                  │
     │4. 302 Redirect to /auth/callback?code=AUTH_CODE                    │
     │<──────────────────────────────────────────────┤                  │
     │               │                │                │                  │
     │5. GET /auth/callback?code=AUTH_CODE            │                  │
     ├──────────────>│                │                │                  │
     │               │                │                │                  │
     │               │6. Exchange Code for Tokens (code_verifier)         │
     │               │   POST /token (client_id, client_secret, code)     │
     │               ├───────────────────────────────>│                  │
     │               │                │                │                  │
     │               │7. {access_token, refresh_token, id_token}          │
     │               │<───────────────────────────────┤                  │
     │               │                │                │                  │
     │               │8. Generate SESSION_ID = UUID   │                  │
     │               │   Store SESSION:{id} → Tokens  │                  │
     │               ├───────────────>│                │                  │
     │               │                │                │                  │
     │9. 302 Redirect to App                          │                  │
     │   Set-Cookie: SESSION_ID={id}; HttpOnly; Secure; SameSite=Strict  │
     │<──────────────┤                │                │                  │
     │               │                │                │                  │
```

### Phase 2: Secured API Call (Transparent Auth)

```
     │               │                │                │                  │
     │10. GET /api/users (Cookie: SESSION_ID)         │                  │
     ├──────────────>│                │                │                  │
     │               │                │                │                  │
     │               │11. Check L1 Cache (SESSION_ID → Token)             │
     │               │   [Cache Hit]  │                │                  │
     │               │                │                │                  │
     │               │   [Cache Miss] │                │                  │
     │               │   GET SESSION:{id}              │                  │
     │               ├───────────────>│                │                  │
     │               │                │                │                  │
     │               │   {access_token, refresh_token, expires_at}        │
     │               │<───────────────┤                │                  │
     │               │                │                │                  │
     │               │12. Check Token Expiry           │                  │
     │               │   [IF Expired] │                │                  │
     │               │   Refresh Token│                │                  │
     │               ├───────────────────────────────>│                  │
     │               │                │                │                  │
     │               │   New Tokens   │                │                  │
     │               │<───────────────────────────────┤                  │
     │               │                │                │                  │
     │               │   UPDATE SESSION:{id}          │                  │
     │               ├───────────────>│                │                  │
     │               │                │                │                  │
     │               │13. Forward Request with JWT    │                  │
     │               │   Authorization: Bearer {access_token}             │
     │               ├────────────────────────────────────────────────────>│
     │               │                │                │                  │
     │               │                │                │14. Validate JWT  │
     │               │                │                │   Verify Signature
     │               │                │                │   Extract Claims │
     │               │                │                │                  │
     │               │15. Response    │                │                  │
     │               │<────────────────────────────────────────────────────┤
     │               │                │                │                  │
     │16. Response   │                │                │                  │
     │<──────────────┤                │                │                  │
```

---

## 3. Risk Analysis & Solutions

| Risk | Impact | Solution | Priority |
|------|--------|----------|----------|
| **CSRF (Cross-Site Request Forgery)** | Attacker forces user browser to execute actions (using the auto-sent cookie). | **1. SameSite=Strict** Cookie Attribute (Prevents external triggering).<br>**2. Anti-CSRF Pattern**: Gateway requires a custom header (`X-XSRF-TOKEN` or `X-Requested-With`) for mutating requests (POST, PUT, DELETE). | **HIGH** |
| **BFF Scalability (Redis Bottleneck)** | Every API call requires a Session lookup in Redis. At 1M CCU, this is massive load. | **L1 Caching**: Gateway caches `SESSION_ID → Access Token` in Caffeine for short duration (e.g., 60s). This reduces Redis reads by **99%** for active users. | **CRITICAL** |
| **State Synchronization** | If Gateway 1 refreshes token, Gateway 2 might see old data if L1 cache persists. | **Short L1 TTL**: Keep L1 duration < Access Token lifespan. Accept occasional double-refresh (idempotent at IdP) or use Redis Pub/Sub for invalidation (complex). **Simple short TTL is preferred**. | **MEDIUM** |
| **XSS (Cross-Site Scripting)** | Malicious JS steals Session. | **HttpOnly Cookie**: JS cannot read the cookie. This makes Session stealing significantly harder than stealing localStorage tokens. | **HIGH** |
| **Session Fixation** | Attacker sets a known Session ID before user login. | **Regenerate Session**: On successful login callback, Gateway MUST generate a completely new SESSION_ID. | **HIGH** |
| **Token Leakage** | Access token exposed in logs, monitoring. | **1. Never log tokens**.<br>**2. Use trace logging only in dev**.<br>**3. Redact Authorization headers in production logs**. | **MEDIUM** |
| **Session Hijacking** | Stolen cookie used from different location. | **1. Bind session to IP (optional, breaks mobile)**.<br>**2. Rotate SESSION_ID periodically**.<br>**3. Short session TTL (15-30 min)**. | **MEDIUM** |

---

## 4. API Specification (BFF Layer)

These APIs are hosted on `gateway-service` under `/api/auth` or `/auth`.

### 4.1. GET `/auth/login`

**Description**: Initiates Login.

**Request**:
```http
GET /auth/login?redirect_uri=/dashboard HTTP/1.1
Host: gateway.enterprise.com
```

**Response**:
```http
HTTP/1.1 302 Found
Location: https://keycloak.enterprise.com/realms/enterprise/protocol/openid-connect/auth?
  response_type=code&
  client_id=enterprise-gateway&
  redirect_uri=https://gateway.enterprise.com/auth/callback&
  scope=openid profile email&
  state=random_state_value&
  code_challenge=BASE64URL(SHA256(code_verifier))&
  code_challenge_method=S256
```

---

### 4.2. GET `/auth/callback`

**Description**: OIDC Callback. Logic: Code swap → Session Create → Cookie Set.

**Request**:
```http
GET /auth/callback?code=AUTH_CODE&state=random_state_value HTTP/1.1
Host: gateway.enterprise.com
```

**Internal Flow**:
1. Validate `state` parameter (CSRF protection)
2. Exchange `code` for tokens using `code_verifier` (PKCE)
3. Generate new `SESSION_ID = UUID`
4. Store `SESSION:{id} → {access_token, refresh_token, expires_at}` in Redis (TTL: 30min)
5. Set HttpOnly cookie

**Response**:
```http
HTTP/1.1 302 Found
Location: /dashboard
Set-Cookie: SESSION_ID=550e8400-e29b-41d4-a716-446655440000;
            Path=/;
            HttpOnly;
            Secure;
            SameSite=Strict;
            Max-Age=1800
```

---

### 4.3. POST `/auth/logout`

**Description**: Global Logout. Logic: Del Redis, Revoke at Keycloak, Clear Cookie.

**Request**:
```http
POST /auth/logout HTTP/1.1
Host: gateway.enterprise.com
Cookie: SESSION_ID=550e8400-e29b-41d4-a716-446655440000
X-XSRF-TOKEN: csrf_token_value
```

**Internal Flow**:
1. Lookup session in Redis
2. Revoke tokens at Keycloak (optional, best-effort)
3. Delete `SESSION:{id}` from Redis
4. Clear cookie

**Response**:
```http
HTTP/1.1 200 OK
Set-Cookie: SESSION_ID=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0

{
  "message": "Logged out successfully"
}
```

---

### 4.4. GET `/auth/user`

**Description**: Get User Profile (for UI display). Validates Session, returns claims.

**Request**:
```http
GET /auth/user HTTP/1.1
Host: gateway.enterprise.com
Cookie: SESSION_ID=550e8400-e29b-41d4-a716-446655440000
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "email": "john.doe@enterprise.com",
  "name": "John Doe",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "authenticated": true
}
```

**Error Response** (Session invalid/expired):
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "UNAUTHORIZED",
  "message": "Session expired or invalid"
}
```

---

### 4.5. GET `/auth/status`

**Description**: Heartbeat check.

**Request**:
```http
GET /auth/status HTTP/1.1
Host: gateway.enterprise.com
Cookie: SESSION_ID=550e8400-e29b-41d4-a716-446655440000
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "authenticated": true,
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 1234,
  "csrf": "csrf_token_value"
}
```

---

## 5. Gateway Implementation Logic

### 5.1. Dependencies

```xml
<!-- OAuth2 Client for OIDC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Reactive Gateway -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- Redis Reactive -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>

<!-- Caffeine for L1 Cache -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 5.2. SecurityWebFilterChain

```java
@Bean
public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
            .requireCsrfProtectionMatcher(new RequireCsrfProtectionMatcher())
        )
        .oauth2Login(oauth2 -> oauth2
            .authenticationSuccessHandler(new SessionCreatingSuccessHandler(redisTemplate))
        )
        .authorizeExchange(auth -> auth
            .pathMatchers("/auth/login", "/auth/callback", "/actuator/**").permitAll()
            .anyExchange().authenticated()
        );

    return http.build();
}
```

### 5.3. Session Creation on Login Success

```java
public class SessionCreatingSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ReactiveRedisTemplate<String, SessionData> redisTemplate;

    @Override
    public Mono<Void> onAuthenticationSuccess(
        WebFilterExchange webFilterExchange,
        Authentication authentication
    ) {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = // ... obtain from repository

        // Extract tokens
        String accessToken = client.getAccessToken().getTokenValue();
        String refreshToken = client.getRefreshToken().getTokenValue();
        Instant expiresAt = client.getAccessToken().getExpiresAt();

        // Generate session
        String sessionId = UUID.randomUUID().toString();
        SessionData sessionData = new SessionData(accessToken, refreshToken, expiresAt);

        // Store in Redis
        return redisTemplate
            .opsForValue()
            .set("SESSION:" + sessionId, sessionData, Duration.ofMinutes(30))
            .then(Mono.defer(() -> {
                // Set cookie
                ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofMinutes(30))
                    .build();

                webFilterExchange.getExchange().getResponse()
                    .addCookie(cookie);

                return webFilterExchange.getExchange().getResponse()
                    .setComplete();
            }));
    }
}
```

### 5.4. GlobalFilter for JWT Enrichment

```java
@Component
@Order(-1)
public class JwtEnrichmentFilter implements GlobalFilter {

    private final ReactiveRedisTemplate<String, SessionData> redisTemplate;
    private final Cache<String, String> l1Cache; // Caffeine

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract SESSION_ID from cookie
        String sessionId = extractSessionId(exchange);

        if (sessionId == null) {
            return chain.filter(exchange); // No session, continue
        }

        // L1 Cache lookup
        String cachedToken = l1Cache.getIfPresent(sessionId);
        if (cachedToken != null) {
            return enrichRequestAndContinue(exchange, chain, cachedToken);
        }

        // L2 Redis lookup
        return redisTemplate.opsForValue()
            .get("SESSION:" + sessionId)
            .flatMap(sessionData -> {
                // Check if token expired
                if (sessionData.isExpired()) {
                    return refreshToken(sessionData, sessionId)
                        .flatMap(newToken -> {
                            l1Cache.put(sessionId, newToken);
                            return enrichRequestAndContinue(exchange, chain, newToken);
                        });
                } else {
                    l1Cache.put(sessionId, sessionData.getAccessToken());
                    return enrichRequestAndContinue(exchange, chain, sessionData.getAccessToken());
                }
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Session not found
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }));
    }

    private Mono<Void> enrichRequestAndContinue(
        ServerWebExchange exchange,
        GatewayFilterChain chain,
        String accessToken
    ) {
        ServerHttpRequest mutatedRequest = exchange.getRequest()
            .mutate()
            .header("Authorization", "Bearer " + accessToken)
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
```

---

## 6. Performance Optimization for 1M CCU

### 6.1. L1 Caffeine Cache Configuration

```java
@Configuration
public class CacheConfiguration {

    @Bean
    public Cache<String, String> sessionTokenCache() {
        return Caffeine.newBuilder()
            .maximumSize(100_000) // Adjust based on memory
            .expireAfterWrite(60, TimeUnit.SECONDS) // Short TTL
            .recordStats()
            .build();
    }
}
```

**Impact**:
- **Cache Hit Rate**: 95%+ for active users
- **Redis Load Reduction**: 99% (from 1M req/s to 10K req/s)
- **Latency**: L1 hit ~1µs vs Redis ~1ms (1000x faster)

### 6.2. Redis Connection Pooling

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 200
          max-idle: 50
          min-idle: 10
```

### 6.3. Horizontal Scaling

- Deploy 10+ Gateway instances behind load balancer
- Each Gateway has independent L1 cache (acceptable for 60s TTL)
- Redis cluster for session store (3 master + 3 replica)

### 6.4. Metrics to Monitor

```java
// L1 Cache Stats
CacheStats stats = sessionTokenCache.stats();
double hitRate = stats.hitRate(); // Target: > 95%

// Redis Performance
RedisInfo info = redisTemplate.getConnectionFactory().getConnection().info();
// Monitor: ops/sec, latency, memory usage
```

---

## 7. Security Considerations

### 7.1. Cookie Security Checklist

- ✅ **HttpOnly**: Prevents JavaScript access
- ✅ **Secure**: Only transmitted over HTTPS
- ✅ **SameSite=Strict**: CSRF protection
- ✅ **Short Max-Age**: 15-30 minutes
- ✅ **Domain restriction**: Specific domain only

### 7.2. CSRF Protection

```java
public class RequireCsrfProtectionMatcher implements ServerWebExchangeMatcher {

    private static final Set<HttpMethod> MUTATING_METHODS = Set.of(
        HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH
    );

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();

        // Require CSRF for mutating requests (except auth callbacks)
        if (MUTATING_METHODS.contains(method) && !path.startsWith("/auth/callback")) {
            return MatchResult.match();
        }

        return MatchResult.notMatch();
    }
}
```

### 7.3. Token Refresh Strategy

```java
private Mono<String> refreshToken(SessionData sessionData, String sessionId) {
    return webClient.post()
        .uri(keycloakTokenUrl)
        .bodyValue(Map.of(
            "grant_type", "refresh_token",
            "refresh_token", sessionData.getRefreshToken(),
            "client_id", clientId,
            "client_secret", clientSecret
        ))
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .flatMap(response -> {
            // Update session in Redis
            SessionData updatedSession = new SessionData(
                response.getAccessToken(),
                response.getRefreshToken(),
                Instant.now().plusSeconds(response.getExpiresIn())
            );

            return redisTemplate.opsForValue()
                .set("SESSION:" + sessionId, updatedSession, Duration.ofMinutes(30))
                .thenReturn(response.getAccessToken());
        });
}
```

### 7.4. Session Timeout Handling

```java
// Client-side (Angular): Heartbeat every 5 minutes
setInterval(() => {
  http.get('/auth/status').subscribe(
    status => {
      if (!status.authenticated) {
        router.navigate(['/login']);
      }
    },
    error => router.navigate(['/login'])
  );
}, 5 * 60 * 1000);
```

---

## Conclusion

This architecture offers the **highest security** (Tokens hidden from browser) while maintaining **1M CCU performance** via:

1. **BFF Pattern**: Gateway acts as confidential client, client only holds session cookie
2. **L1 Caching**: 99% cache hit rate reduces Redis load by 1000x
3. **Stateless JWTs**: Microservices validate tokens without calling IAM
4. **CSRF Protection**: SameSite cookies + custom headers
5. **Automatic Token Refresh**: Transparent to client

**Key Metrics Targets**:
- Session lookup latency: p99 < 5ms
- L1 cache hit rate: > 95%
- Token refresh success rate: > 99.9%
- Gateway throughput: 10K req/s per instance
