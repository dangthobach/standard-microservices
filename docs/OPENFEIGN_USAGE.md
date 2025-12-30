# OpenFeign Client Usage Guide

This guide explains how to use the OpenFeign client infrastructure provided by `common-lib` for microservice-to-microservice and external API communication.

## Table of Contents
1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Authentication Strategies](#authentication-strategies)
4. [Configuration](#configuration)
5. [Error Handling](#error-handling)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

---

## Overview

The `common-lib` provides a standardized Feign client setup with:

- **Pluggable Authentication**: JWT forwarding, API Key, Basic Auth
- **Unified Error Handling**: Automatic error decoding and exception mapping
- **Configurable Timeouts**: Connection and read timeouts
- **Retry Policies**: Configurable retry behavior
- **Logging Support**: Multiple logging levels

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Business Service                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Controller                                           │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    ↓                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Feign Client (IamServiceClient)                      │   │
│  │ - FeignClientConfiguration (base)                    │   │
│  │ - JwtForwardingInterceptor (auth)                    │   │
│  │ - GlobalFeignErrorDecoder (errors)                   │   │
│  └─────────────────┬────────────────────────────────────┘   │
└────────────────────┼────────────────────────────────────────┘
                     ↓ HTTP + JWT
         ┌───────────────────────────┐
         │   IAM Service             │
         │   (receives JWT)          │
         └───────────────────────────┘
```

---

## Quick Start

### Step 1: Enable Feign Clients

Add `@EnableFeignClients` to your main application class:

```java
@SpringBootApplication
@EnableFeignClients
public class BusinessServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
```

### Step 2: Create a Feign Client Interface

```java
@FeignClient(
    name = "iam-service",
    url = "${app.services.iam.url}",
    configuration = IamServiceFeignConfiguration.class
)
public interface IamServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId);
}
```

### Step 3: Create Feign Configuration

```java
@Configuration
@Import(FeignClientConfiguration.class)  // Import base config
public class IamServiceFeignConfiguration {

    @Bean
    public AuthenticationInterceptor jwtForwardingInterceptor() {
        return new JwtForwardingInterceptor();
    }
}
```

### Step 4: Use the Client

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final IamServiceClient iamServiceClient;

    public UserResponse getUser(String userId) {
        return iamServiceClient.getUserById(userId);
    }
}
```

---

## Authentication Strategies

### 1. JWT Forwarding (Microservice-to-Microservice)

**Use Case**: Internal service communication where JWT needs to be propagated.

```java
@Configuration
@Import(FeignClientConfiguration.class)
public class IamServiceFeignConfiguration {

    @Bean
    public AuthenticationInterceptor jwtForwardingInterceptor() {
        return new JwtForwardingInterceptor();
    }
}
```

**How it works**:
1. Extracts JWT from `SecurityContextHolder`
2. Adds `Authorization: Bearer {token}` header
3. Downstream service validates the JWT

**Requirements**:
- Spring Security must be configured
- JWT must be present in security context

---

### 2. API Key Authentication (External APIs)

**Use Case**: Third-party API calls requiring API key authentication.

```java
@Configuration
@Import(FeignClientConfiguration.class)
public class ExternalApiFeignConfiguration {

    @Bean
    public AuthenticationInterceptor apiKeyInterceptor(
        @Value("${app.services.external.api-key}") String apiKey
    ) {
        return new ApiKeyInterceptor("X-API-Key", apiKey);
    }
}
```

**Configuration** (application.yml):
```yaml
app:
  services:
    external:
      url: https://api.example.com
      api-key: ${EXTERNAL_API_KEY:demo-key}
```

**Supported Headers**:
- Default: `X-API-Key`
- Custom: Specify in constructor

```java
// Custom header name
new ApiKeyInterceptor("Authorization", "ApiKey " + apiKey);
```

---

### 3. Basic Authentication

**Use Case**: Legacy services using HTTP Basic Auth.

```java
@Configuration
@Import(FeignClientConfiguration.class)
public class LegacyServiceFeignConfiguration {

    @Bean
    public AuthenticationInterceptor basicAuthInterceptor(
        @Value("${app.services.legacy.username}") String username,
        @Value("${app.services.legacy.password}") String password
    ) {
        return new BasicAuthInterceptor(username, password);
    }
}
```

**Configuration** (application.yml):
```yaml
app:
  services:
    legacy:
      url: http://legacy-system:8080
      username: ${LEGACY_USERNAME:admin}
      password: ${LEGACY_PASSWORD:secret}
```

---

## Configuration

### Timeout Configuration

Default timeouts are defined in `application.yml`:

```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 5000    # 5 seconds
        read-timeout: 10000      # 10 seconds
        follow-redirects: true
```

**Per-Client Override**:
```yaml
feign:
  client:
    config:
      iam-service:              # Client name
        connect-timeout: 3000
        read-timeout: 15000
```

### Retry Configuration

Default: **No retry** (fail-fast).

**Enable Retry**:
```java
@Configuration
public class MyFeignConfiguration extends FeignClientConfiguration {

    @Bean
    @Override
    public Retryer retryer() {
        return new Retryer.Default(
            100,    // Initial backoff (ms)
            1000,   // Max backoff (ms)
            3       // Max attempts
        );
    }
}
```

**Recommendation**: Use **Resilience4j Circuit Breaker** instead for production:
```java
@FeignClient(name = "iam-service", fallback = IamServiceFallback.class)
```

### Logging Configuration

Feign supports 4 logging levels:

```yaml
logging:
  level:
    com.enterprise.business.client.IamServiceClient: DEBUG
```

**Logging Levels**:
- `NONE`: No logging (default)
- `BASIC`: Log method, URL, status, execution time
- `HEADERS`: Log request/response headers
- `FULL`: Log everything (headers + body)

**Configure in Feign Config**:
```java
@Bean
public Logger.Level feignLoggerLevel() {
    return Logger.Level.FULL;  // For debugging
}
```

---

## Error Handling

The `GlobalFeignErrorDecoder` automatically converts HTTP errors to typed exceptions:

| HTTP Status | Exception | Description |
|-------------|-----------|-------------|
| 400 | `BusinessException(BAD_REQUEST)` | Bad request |
| 401 | `FeignUnauthorizedException` | Unauthorized |
| 403 | `FeignForbiddenException` | Forbidden |
| 404 | `FeignNotFoundException` | Not found |
| 409 | `BusinessException(CONFLICT)` | Conflict |
| 422 | `BusinessException(VALIDATION_ERROR)` | Validation error |
| 429 | `FeignRateLimitException` | Rate limit exceeded |
| 5xx | `FeignServerException` | Server error |

### Handling Errors in Your Code

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final IamServiceClient iamServiceClient;

    public Optional<UserResponse> getUser(String userId) {
        try {
            return Optional.of(iamServiceClient.getUserById(userId));
        } catch (FeignNotFoundException e) {
            log.warn("User not found: {}", userId);
            return Optional.empty();
        } catch (FeignUnauthorizedException e) {
            log.error("Unauthorized access to IAM service", e);
            throw new BusinessException("AUTH_ERROR", "Not authorized");
        } catch (FeignServerException e) {
            log.error("IAM service error", e);
            throw new BusinessException("SERVICE_ERROR", "IAM service unavailable");
        }
    }
}
```

### Custom Error Messages

The error decoder extracts error messages from response bodies:

**JSON Response**:
```json
{
  "message": "User not found",
  "error": "NOT_FOUND"
}
```

**Extracted Message**: "User not found"

Supported fields: `message`, `error`, `errorMessage`, `detail`, `title`

---

## Best Practices

### 1. Use Interface Segregation

Create focused client interfaces for specific functionality:

```java
// Good: Focused interface
@FeignClient(name = "iam-users", url = "${iam.url}", path = "/api/users")
public interface IamUserClient {
    @GetMapping("/{id}")
    UserResponse getUser(@PathVariable String id);
}

// Bad: Kitchen-sink interface
@FeignClient(name = "iam", url = "${iam.url}")
public interface IamClient {
    @GetMapping("/api/users/{id}")
    UserResponse getUser(@PathVariable String id);

    @GetMapping("/api/roles/{id}")
    RoleResponse getRole(@PathVariable String id);

    // ... 50 more methods
}
```

### 2. Use DTOs, Not Entities

Never use JPA entities in Feign clients:

```java
// Good: Use DTOs
public class UserResponse {
    private String id;
    private String username;
}

// Bad: Use entities
@Entity
public class User {
    @Id
    private Long id;
    // ...
}
```

### 3. Handle Null Responses

Always handle null or empty responses:

```java
public Optional<UserResponse> getUser(String userId) {
    try {
        UserResponse user = iamServiceClient.getUserById(userId);
        return Optional.ofNullable(user);
    } catch (FeignException e) {
        return Optional.empty();
    }
}
```

### 4. Use Circuit Breakers

For production, always use circuit breakers:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-feign</artifactId>
</dependency>
```

```java
@FeignClient(
    name = "iam-service",
    fallback = IamServiceFallback.class
)
```

### 5. Externalize Configuration

Never hardcode URLs or credentials:

```java
// Good: Externalized
@FeignClient(
    name = "iam-service",
    url = "${app.services.iam.url}"
)

// Bad: Hardcoded
@FeignClient(
    name = "iam-service",
    url = "http://localhost:8081"
)
```

---

## Examples

### Example 1: Internal Service Call with JWT Forwarding

**Feign Client**:
```java
@FeignClient(
    name = "iam-service",
    url = "${app.services.iam.url}",
    configuration = IamServiceFeignConfiguration.class
)
public interface IamServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId);

    @PostMapping("/api/users")
    UserResponse createUser(@RequestBody CreateUserRequest request);
}
```

**Configuration**:
```java
@Configuration
@Import(FeignClientConfiguration.class)
public class IamServiceFeignConfiguration {

    @Bean
    public AuthenticationInterceptor jwtForwardingInterceptor() {
        return new JwtForwardingInterceptor();
    }
}
```

**Usage**:
```java
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final IamServiceClient iamServiceClient;

    public void processUser(String userId) {
        // JWT is automatically forwarded from current request
        UserResponse user = iamServiceClient.getUserById(userId);

        // Business logic
        log.info("Processing user: {}", user.getUsername());
    }
}
```

---

### Example 2: External API Call with API Key

**Feign Client**:
```java
@FeignClient(
    name = "weather-api",
    url = "${app.services.weather.url}",
    configuration = WeatherApiFeignConfiguration.class
)
public interface WeatherApiClient {

    @GetMapping("/current")
    WeatherResponse getCurrentWeather(@RequestParam("city") String city);
}
```

**Configuration**:
```java
@Configuration
@Import(FeignClientConfiguration.class)
public class WeatherApiFeignConfiguration {

    @Bean
    public AuthenticationInterceptor apiKeyInterceptor(
        @Value("${app.services.weather.api-key}") String apiKey
    ) {
        return new ApiKeyInterceptor("X-API-Key", apiKey);
    }
}
```

**application.yml**:
```yaml
app:
  services:
    weather:
      url: https://api.weatherapi.com/v1
      api-key: ${WEATHER_API_KEY}
```

---

### Example 3: Multiple Interceptors

You can combine multiple interceptors:

```java
@Configuration
@Import(FeignClientConfiguration.class)
public class CustomFeignConfiguration {

    @Bean
    public RequestInterceptor userAgentInterceptor() {
        return template -> {
            template.header("User-Agent", "Enterprise-Service/1.0");
        };
    }

    @Bean
    public AuthenticationInterceptor apiKeyInterceptor(
        @Value("${api.key}") String apiKey
    ) {
        return new ApiKeyInterceptor(apiKey);
    }
}
```

---

## Testing Feign Clients

### Unit Testing with WireMock

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableFeignClients
class IamServiceClientTest {

    @Autowired
    private IamServiceClient client;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.services.iam.url", wireMock::baseUrl);
    }

    @Test
    void shouldGetUserById() {
        wireMock.stubFor(get("/api/users/123")
            .willReturn(okJson("{\"id\":\"123\",\"username\":\"john\"}")));

        UserResponse user = client.getUserById("123");

        assertThat(user.getId()).isEqualTo("123");
        assertThat(user.getUsername()).isEqualTo("john");
    }
}
```

---

## Troubleshooting

### Common Issues

**1. `FeignClientConfiguration not found`**
- Ensure `common-lib` is added as a dependency
- Check that OpenFeign is enabled: `@EnableFeignClients`

**2. `No Feign Client for loadBalancing defined`**
- Add `url` attribute to `@FeignClient` if not using service discovery

**3. `401 Unauthorized` on internal calls**
- Verify JWT is present in SecurityContext
- Check `JwtForwardingInterceptor` is configured
- Enable DEBUG logging to see headers

**4. Connection timeouts**
- Increase `connect-timeout` and `read-timeout`
- Check network connectivity
- Verify target service is running

---

## Related Documentation

- [AUTHZ_WORKFLOW.md](AUTHZ_WORKFLOW.md) - BFF Authentication Pattern
- [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Feign GitHub](https://github.com/OpenFeign/feign)
