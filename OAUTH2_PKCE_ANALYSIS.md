# OAuth2 PKCE Flow Analysis - Current State & Gaps

## Kiểm Tra Yêu Cầu

Bạn muốn workflow sau:
```
Client (Frontend)
  → Gateway redirect to Keycloak (Authorization Code Flow with PKCE)
  → Keycloak authentication
  → Keycloak returns authorization code
  → Frontend exchanges code for access token
  → Gateway validates access token (JWT)
  → Gateway creates SESSION_ID
  → Response SESSION_ID to client
```

---

## 1. Current State - Gateway Service ✅ Có / ❌ Thiếu

### ✅ Có sẵn:

#### 1.1. JWT Resource Server Configuration
[application.yml](gateway-service/src/main/resources/application.yml#L73-L79)
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/enterprise
          jwk-set-uri: http://localhost:8180/realms/enterprise/protocol/openid-connect/certs
```

**Chức năng**:
- Gateway validate JWT token từ Keycloak
- Tự động verify signature sử dụng JWK (JSON Web Key)
- Check token expiration, issuer, audience

#### 1.2. Security Configuration
[SecurityConfiguration.java](gateway-service/src/main/java/com/enterprise/gateway/config/SecurityConfiguration.java)

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/actuator/**", "/health/**").permitAll()
                .pathMatchers("/auth/**", "/public/**").permitAll()
                .pathMatchers("/api/**").authenticated()  // ✅ Require JWT
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
            )
            .build();
    }
}
```

**Chức năng**:
- ✅ OAuth2 Resource Server mode (validate JWT)
- ✅ CORS configuration (support cross-origin)
- ✅ Public endpoints: `/auth/**`, `/public/**`
- ✅ Protected endpoints: `/api/**` (require JWT)

#### 1.3. CORS Configuration
```java
.setAllowedOriginPatterns(List.of("*"))
.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"))
.setAllowCredentials(true)
.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id"))
```

**Chức năng**:
- ✅ Allow credentials (cookies, session)
- ✅ Expose Authorization header
- ✅ Support all HTTP methods

### ❌ Thiếu:

#### 1.1. ❌ OAuth2 Login Configuration (PKCE Flow)
Gateway hiện tại chỉ là **Resource Server** (validate JWT), KHÔNG có **OAuth2 Login** (redirect to Keycloak).

**Cần thêm**:
```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .oauth2Login(oauth2 -> oauth2
            .authorizationRequestResolver(pkceAuthorizationRequestResolver())
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt())
        .build();
}

// PKCE Support
private ReactiveAuthorizationRequestResolver pkceAuthorizationRequestResolver() {
    WebClientReactiveAuthorizationCodeTokenResponseClient client =
        new WebClientReactiveAuthorizationCodeTokenResponseClient();
    client.setParametersConverter(new PkceParametersConverter());
    return new DefaultServerOAuth2AuthorizationRequestResolver(...);
}
```

#### 1.2. ❌ Session Management (SESSION_ID)
Gateway không có session management. Hiện tại chỉ validate JWT cho mỗi request (stateless).

**Cần thêm**:
```java
// Session configuration
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)
.sessionConcurrency(concurrency -> concurrency
    .maximumSessions(1)
    .maxSessionsPreventsLogin(false)
)
```

Sau khi login thành công, tạo SESSION_ID và trả về cookie:
```java
@PostMapping("/auth/login/callback")
public Mono<ResponseEntity<LoginResponse>> handleCallback(
    @RequestParam String code,
    ServerWebExchange exchange
) {
    return exchangeCodeForToken(code)
        .flatMap(tokenResponse -> {
            // Create session
            String sessionId = UUID.randomUUID().toString();

            // Store session in Redis
            sessionRepository.save(sessionId, tokenResponse.getAccessToken());

            // Set cookie
            ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(Duration.ofHours(24))
                .path("/")
                .build();

            exchange.getResponse().addCookie(cookie);

            return Mono.just(ResponseEntity.ok(new LoginResponse(sessionId)));
        });
}
```

#### 1.3. ❌ OAuth2 Client Configuration
Thiếu configuration để Gateway hoạt động như OAuth2 Client (exchange code for token).

**Cần thêm vào `application.yml`**:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: gateway-service
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/enterprise
            authorization-uri: http://localhost:8180/realms/enterprise/protocol/openid-connect/auth
            token-uri: http://localhost:8180/realms/enterprise/protocol/openid-connect/token
            user-info-uri: http://localhost:8180/realms/enterprise/protocol/openid-connect/userinfo
            jwk-set-uri: http://localhost:8180/realms/enterprise/protocol/openid-connect/certs
            user-name-attribute: preferred_username
```

---

## 2. Current State - Frontend ✅ Có / ❌ Thiếu

### ✅ Có sẵn:

#### 2.1. Environment Configuration
[environment.ts](frontend/src/environments/environment.ts)

```typescript
export const environment = {
  apiUrl: 'http://localhost:8080/api',
  keycloak: {
    issuer: 'http://localhost:8180/realms/enterprise',
    clientId: 'enterprise-frontend',
    redirectUri: window.location.origin,
    scope: 'openid profile email',
    responseType: 'code',       // ✅ Authorization Code
    usePkce: true,              // ✅ PKCE enabled
    showDebugInformation: true,
    requireHttps: false
  }
}
```

**Chức năng**:
- ✅ Keycloak configuration
- ✅ PKCE enabled
- ✅ Authorization Code flow

### ❌ Thiếu:

#### 2.1. ❌ OAuth2 Library (angular-oauth2-oidc)
Frontend CHƯA có library để thực hiện OAuth2 flow.

**Cần cài đặt**:
```bash
npm install angular-oauth2-oidc
```

#### 2.2. ❌ Auth Service
Chưa có service để handle authentication flow.

**Cần tạo**: `frontend/src/app/core/services/auth.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private authConfig: AuthConfig = {
    issuer: environment.keycloak.issuer,
    redirectUri: environment.keycloak.redirectUri,
    clientId: environment.keycloak.clientId,
    responseType: 'code',
    scope: environment.keycloak.scope,
    showDebugInformation: environment.keycloak.showDebugInformation,
    requireHttps: environment.keycloak.requireHttps,
    useSilentRefresh: true,
    silentRefreshRedirectUri: window.location.origin + '/silent-refresh.html',

    // PKCE Configuration
    oidc: true,
    // This automatically enables PKCE
  };

  constructor(
    private oauthService: OAuthService,
    private router: Router
  ) {
    this.configureOAuth();
  }

  private configureOAuth() {
    this.oauthService.configure(this.authConfig);
    this.oauthService.setupAutomaticSilentRefresh();
  }

  async login() {
    this.oauthService.initCodeFlow();
  }

  async handleCallback() {
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();

    if (this.oauthService.hasValidAccessToken()) {
      const sessionId = await this.exchangeTokenForSession();
      localStorage.setItem('SESSION_ID', sessionId);
      this.router.navigate(['/dashboard']);
    }
  }

  async exchangeTokenForSession(): Promise<string> {
    const accessToken = this.oauthService.getAccessToken();

    const response = await fetch(`${environment.apiUrl}/auth/session`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();
    return data.sessionId;
  }

  logout() {
    localStorage.removeItem('SESSION_ID');
    this.oauthService.logOut();
  }

  getAccessToken(): string {
    return this.oauthService.getAccessToken();
  }

  getSessionId(): string | null {
    return localStorage.getItem('SESSION_ID');
  }

  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }
}
```

#### 2.3. ❌ Auth Guard
Chưa có guard để protect routes.

**Cần tạo**: `frontend/src/app/core/guards/auth.guard.ts`

```typescript
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Store the attempted URL for redirecting after login
  localStorage.setItem('redirectUrl', state.url);

  // Redirect to login
  router.navigate(['/login']);
  return false;
};
```

#### 2.4. ❌ HTTP Interceptor
Chưa có interceptor để attach JWT token vào requests.

**Cần tạo**: `frontend/src/app/core/interceptors/auth.interceptor.ts`

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();
  const sessionId = authService.getSessionId();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        ...(sessionId && { 'X-Session-Id': sessionId })
      }
    });
  }

  return next(req);
};
```

---

## 3. Complete PKCE Flow - Thiếu & Cần Implement

### Workflow Hoàn Chỉnh:

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Frontend   │         │   Gateway   │         │  Keycloak   │
│  (Angular)  │         │  (WebFlux)  │         │   (Auth)    │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       │ 1. User clicks Login  │                       │
       ├──────────────────────>│                       │
       │                       │                       │
       │ 2. Generate code_verifier (PKCE)              │
       │    Generate code_challenge                    │
       │                       │                       │
       │ 3. Redirect to /auth/login                    │
       ├──────────────────────>│                       │
       │                       │                       │
       │                       │ 4. Redirect to Keycloak with:
       │                       │    - client_id         │
       │                       │    - redirect_uri      │
       │                       │    - scope             │
       │                       │    - response_type=code│
       │                       │    - code_challenge    │
       │                       │    - code_challenge_method=S256
       │                       ├──────────────────────>│
       │                       │                       │
       │ 5. Login page         │                       │
       │<──────────────────────────────────────────────┤
       │                       │                       │
       │ 6. Enter credentials  │                       │
       ├──────────────────────────────────────────────>│
       │                       │                       │
       │                       │ 7. Validate credentials│
       │                       │    Generate auth code │
       │                       │                       │
       │ 8. Redirect with code │                       │
       │<──────────────────────────────────────────────┤
       │   ?code=abc123...     │                       │
       │                       │                       │
       │ 9. Exchange code for token                    │
       │    POST /token        │                       │
       │    + code             │                       │
       │    + code_verifier    │                       │
       ├──────────────────────────────────────────────>│
       │                       │                       │
       │                       │ 10. Validate code_verifier
       │                       │     hash(code_verifier) == code_challenge?
       │                       │                       │
       │ 11. Access Token + Refresh Token              │
       │<──────────────────────────────────────────────┤
       │    {                  │                       │
       │      "access_token": "eyJhbG...",             │
       │      "refresh_token": "eyJhbG...",            │
       │      "expires_in": 3600                       │
       │    }                  │                       │
       │                       │                       │
       │ 12. POST /auth/session│                       │
       │     Bearer eyJhbG...  │                       │
       ├──────────────────────>│                       │
       │                       │                       │
       │                       │ 13. Validate JWT      │
       │                       │     (verify signature)│
       │                       │                       │
       │                       │ 14. Create SESSION_ID │
       │                       │     Store in Redis    │
       │                       │     session -> token  │
       │                       │                       │
       │ 15. Response SESSION_ID                       │
       │<──────────────────────┤                       │
       │    {                  │                       │
       │      "sessionId": "550e8400-e29b-41d4-a716-446655440000"
       │    }                  │                       │
       │    Set-Cookie: SESSION_ID=550e8400...         │
       │                       │                       │
       │ 16. Store SESSION_ID  │                       │
       │     in localStorage   │                       │
       │                       │                       │
       │ 17. API Request       │                       │
       │     Cookie: SESSION_ID│                       │
       ├──────────────────────>│                       │
       │                       │                       │
       │                       │ 18. Lookup token from session
       │                       │     Validate JWT      │
       │                       │                       │
       │                       │ 19. Forward to service│
       │                       │     + JWT token       │
       │                       │                       │
       │ 20. Response          │                       │
       │<──────────────────────┤                       │
       │                       │                       │
```

---

## 4. Implementation Checklist

### Gateway Service

- [ ] **Add OAuth2 Client dependencies** (spring-boot-starter-oauth2-client)
- [ ] **Add OAuth2 Login configuration** (support PKCE)
- [ ] **Create `/auth/login` endpoint** (trigger OAuth2 flow)
- [ ] **Create `/auth/callback` endpoint** (handle authorization code)
- [ ] **Create `/auth/session` endpoint** (exchange JWT for SESSION_ID)
- [ ] **Add Session Management** (Redis-based)
  - [ ] SessionRepository (store SESSION_ID -> JWT)
  - [ ] SessionFilter (validate SESSION_ID from cookie)
- [ ] **Add PKCE support** (PkceParametersConverter)
- [ ] **Configure session cookies** (HttpOnly, Secure, SameSite)

### Frontend

- [ ] **Install angular-oauth2-oidc**
- [ ] **Create AuthService** (handle OAuth2 flow)
- [ ] **Create AuthGuard** (protect routes)
- [ ] **Create AuthInterceptor** (attach JWT to requests)
- [ ] **Create Login component**
- [ ] **Create Callback component** (handle redirect from Keycloak)
- [ ] **Configure routes** (with authGuard)
- [ ] **Handle token refresh** (silent refresh)
- [ ] **Handle logout** (clear session + revoke token)

### Keycloak Configuration

- [ ] **Create Realm**: `enterprise`
- [ ] **Create Client**: `enterprise-frontend`
  - Client Protocol: openid-connect
  - Access Type: public (for SPA)
  - Valid Redirect URIs: `http://localhost:4200/*`
  - Web Origins: `http://localhost:4200`
  - PKCE: Required (S256)
- [ ] **Create Client**: `gateway-service`
  - Client Protocol: openid-connect
  - Access Type: confidential
  - Valid Redirect URIs: `http://localhost:8080/login/oauth2/code/keycloak`
- [ ] **Create Roles**: ADMIN, USER, MANAGER
- [ ] **Create Test Users**

---

## 5. Gap Summary

| Component | Status | Missing Items |
|-----------|--------|---------------|
| **Gateway** | 🟡 Partial | OAuth2 Login, Session Management, PKCE support |
| **Frontend** | 🔴 Missing | OAuth2 library, AuthService, AuthGuard, Interceptor |
| **Keycloak** | ❓ Unknown | Need to verify client configuration |
| **IAM Service** | 🟢 OK | Has KeycloakService but no session endpoints |

---

## 6. Recommended Next Steps

### Priority 1 (Critical):
1. Install `angular-oauth2-oidc` in frontend
2. Create AuthService in frontend
3. Add OAuth2 Client configuration to Gateway
4. Create session management endpoints in Gateway

### Priority 2 (Important):
5. Create AuthGuard and AuthInterceptor
6. Configure Keycloak clients properly
7. Test PKCE flow end-to-end

### Priority 3 (Nice to have):
8. Implement silent token refresh
9. Add logout functionality
10. Session timeout handling

---

## 7. Security Considerations

✅ **Good practices already in place**:
- JWT validation with JWK
- CORS configured
- HttpOnly cookies support

⚠️ **Need to add**:
- PKCE prevents authorization code interception
- Secure cookie flags (HttpOnly, Secure, SameSite)
- Session expiration management
- Token refresh mechanism
- CSRF protection for session endpoints

---

**Status**: ⚠️ Hệ thống có foundation tốt nhưng thiếu OAuth2 Login flow và Session Management

**Recommendation**: Implement theo thứ tự Priority 1 → 2 → 3 để có workflow hoàn chỉnh.
