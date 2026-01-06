# Consul Service Discovery - Audit Report

## 📋 Tổng quan

Báo cáo này kiểm tra toàn bộ backend repository để đảm bảo tất cả các service giao tiếp với nhau qua Consul Service Discovery.

**Ngày kiểm tra**: 2026-01-07  
**Trạng thái**: ✅ **HOÀN TẤT** - Tất cả service đã được cấu hình đúng

---

## ✅ Trạng thái các Service

### 1. Gateway Service (`gateway-service`)

#### Dependencies
- ✅ `spring-cloud-starter-consul-discovery` - Đã có
- ✅ `spring-cloud-starter-loadbalancer` - **Đã thêm** (cho WebClient)

#### Configuration
- ✅ Consul discovery enabled trong `application.yml`
- ✅ Service registration: `gateway-service`
- ✅ Health check: `/actuator/health`
- ✅ LoadBalanced WebClient configuration: `WebClientLoadBalancerConfiguration`

#### Service Calls
- ✅ `PolicyManager` → `iam-service` (via `lb://iam-service`)
- ✅ `AuthZService` → `iam-service` (via `lb://iam-service`)
- ✅ `UserRoleService` → `iam-service` (via `lb://iam-service`)
- ✅ Gateway routes → Tất cả downstream services (via `lb://service-name`)

**Kết luận**: ✅ **HOÀN TOÀN ĐÚNG** - Tất cả gọi qua Consul với LoadBalancer

---

### 2. IAM Service (`iam-service`)

#### Dependencies
- ✅ `spring-cloud-starter-consul-discovery` - Đã có
- ✅ `spring-cloud-starter-loadbalancer` - **Đã thêm** (cho OpenFeign)
- ✅ `spring-cloud-starter-openfeign` - Đã có

#### Configuration
- ✅ Consul discovery enabled trong `application.yml`
- ✅ Service registration: `iam-service`
- ✅ Health check: `/actuator/health`
- ✅ Port: `8081`

#### Service Calls
- ✅ `KeycloakClient` → External Keycloak (không cần Consul - external service)

**Kết luận**: ✅ **HOÀN TOÀN ĐÚNG** - Đã đăng ký với Consul, sẵn sàng nhận requests

---

### 3. Business Service (`business-service`)

#### Dependencies
- ✅ `spring-cloud-starter-consul-discovery` - Đã có
- ✅ `spring-cloud-starter-loadbalancer` - **Đã thêm** (cho OpenFeign)
- ✅ `spring-cloud-starter-openfeign` - Đã có

#### Configuration
- ✅ Consul discovery enabled trong `application.yml`
- ✅ Service registration: `business-service`
- ✅ Health check: `/actuator/health`
- ✅ Port: `8082`

#### Service Calls
- ✅ `IamServiceClient` → `iam-service` (via service name, **ĐÃ SỬA** - bỏ URL trực tiếp)
- ✅ `ExternalApiClient` → External API (không cần Consul - external service)

**Kết luận**: ✅ **HOÀN TOÀN ĐÚNG** - Đã sửa để dùng service discovery

---

## 🔧 Các Thay Đổi Đã Thực Hiện

### 1. Gateway Service

#### Thêm LoadBalancer Dependency
```xml
<!-- gateway-service/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

#### Tạo WebClient LoadBalancer Configuration
```java
// gateway-service/.../config/WebClientLoadBalancerConfiguration.java
@Bean
@Primary
@LoadBalanced
public WebClient.Builder loadBalancedWebClientBuilder(
        ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter) {
    return WebClient.builder()
            .filter(loadBalancerFilter);
}
```

#### Sửa các Service để dùng LoadBalanced WebClient
- `PolicyManager`: Dùng `lb://iam-service`
- `AuthZService`: Dùng `lb://iam-service`
- `UserRoleService`: Dùng `lb://iam-service`

### 2. Business Service

#### Thêm LoadBalancer Dependency
```xml
<!-- business-service/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

#### Sửa IamServiceClient
```java
// Trước:
@FeignClient(
    name = "iam-service",
    url = "${app.services.iam.url:http://localhost:8081}",  // ❌ URL trực tiếp
    ...
)

// Sau:
@FeignClient(
    name = "iam-service",  // ✅ Service name - resolved via Consul
    // url attribute removed - using service discovery
    ...
)
```

### 3. IAM Service

#### Thêm LoadBalancer Dependency
```xml
<!-- iam-service/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

---

## 📊 Kiến Trúc Service Discovery

```
┌─────────────────────────────────────────────────────────────┐
│                    Consul Server                             │
│              (Service Registry & Discovery)                 │
└───────────────────────┬───────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Gateway    │  │     IAM      │  │   Business   │
│   Service    │  │   Service    │  │   Service    │
│              │  │              │  │              │
│ Port: 8080   │  │ Port: 8081   │  │ Port: 8082   │
│              │  │              │  │              │
│ ✅ Consul    │  │ ✅ Consul    │  │ ✅ Consul    │
│ ✅ LoadBal   │  │ ✅ LoadBal   │  │ ✅ LoadBal   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                  │                  │
       │                  │                  │
       └──────────────────┼──────────────────┘
                          │
                    Service Calls
                    (via Consul)
```

---

## 🔄 Luồng Giao Tiếp

### 1. Gateway → IAM Service

```
Gateway Service
    │
    │ WebClient với @LoadBalanced
    │ baseUrl("lb://iam-service")
    │
    ▼
Consul Server
    │
    │ Resolve "iam-service"
    │ → Lấy danh sách instances
    │ → Load balance (round-robin)
    │
    ▼
IAM Service Instance 1 (8081)
IAM Service Instance 2 (8081)  ← Nếu có nhiều instance
```

### 2. Business Service → IAM Service

```
Business Service
    │
    │ OpenFeign Client
    │ @FeignClient(name = "iam-service")
    │
    ▼
Consul Server
    │
    │ Resolve "iam-service"
    │ → Lấy danh sách instances
    │ → Load balance (round-robin)
    │
    ▼
IAM Service Instance 1 (8081)
IAM Service Instance 2 (8081)  ← Nếu có nhiều instance
```

### 3. Gateway → Business Service (via Gateway Routes)

```
Client Request
    │
    ▼
Gateway Service
    │
    │ Spring Cloud Gateway Route
    │ uri: lb://business-service
    │
    ▼
Consul Server
    │
    │ Resolve "business-service"
    │ → Lấy danh sách instances
    │ → Load balance
    │
    ▼
Business Service Instance 1 (8082)
Business Service Instance 2 (8082)  ← Nếu có nhiều instance
```

---

## ✅ Checklist Hoàn Chỉnh

### Gateway Service
- [x] Consul discovery dependency
- [x] LoadBalancer dependency
- [x] Consul configuration trong application.yml
- [x] Service registration
- [x] LoadBalanced WebClient configuration
- [x] PolicyManager dùng service discovery
- [x] AuthZService dùng service discovery
- [x] UserRoleService dùng service discovery
- [x] Gateway routes dùng `lb://` scheme

### IAM Service
- [x] Consul discovery dependency
- [x] LoadBalancer dependency
- [x] Consul configuration trong application.yml
- [x] Service registration
- [x] Health check endpoint

### Business Service
- [x] Consul discovery dependency
- [x] LoadBalancer dependency
- [x] Consul configuration trong application.yml
- [x] Service registration
- [x] Health check endpoint
- [x] IamServiceClient dùng service discovery (không dùng URL trực tiếp)

---

## 🎯 Lợi Ích

### 1. Service Discovery
- ✅ Tự động phát hiện service instances
- ✅ Không cần hardcode IP/port
- ✅ Dynamic service registration/deregistration

### 2. Load Balancing
- ✅ Tự động phân tải giữa các instances
- ✅ Round-robin mặc định
- ✅ Health check filtering (chỉ route đến healthy instances)

### 3. High Availability
- ✅ Tự động failover khi một instance down
- ✅ Consul tự động remove unhealthy instances
- ✅ Zero-downtime deployment support

### 4. Scalability
- ✅ Dễ dàng scale out (thêm instances)
- ✅ Không cần thay đổi configuration
- ✅ Tự động load balance

---

## 🚀 Testing

### Kiểm tra Service Registration

```bash
# Xem tất cả services đã đăng ký
curl http://localhost:8500/v1/agent/services

# Xem instances của một service
curl http://localhost:8500/v1/health/service/iam-service

# Xem service health
curl http://localhost:8500/v1/health/service/iam-service?passing
```

### Kiểm tra Load Balancing

1. **Start 2 instances của iam-service**:
   ```bash
   # Instance 1
   docker run -p 8081:8081 iam-service
   
   # Instance 2
   docker run -p 8083:8081 iam-service
   ```

2. **Gọi từ Gateway**:
   ```bash
   # Gọi nhiều lần, sẽ thấy requests được phân tải
   for i in {1..10}; do
     curl http://localhost:8080/api/iam/health
   done
   ```

3. **Kiểm tra logs**: Requests sẽ được phân tải giữa 2 instances

---

## 📝 Notes

### External Services
- **Keycloak**: External service, không cần Consul
- **External APIs**: External services, không cần Consul
- **Database/Redis**: Infrastructure, không cần Consul

### Configuration Override
Có thể override service name hoặc URL nếu cần:
```yaml
# application.yml
iam:
  service:
    name: iam-service  # Default
```

---

## ✅ Kết Luận

**TẤT CẢ CÁC SERVICE ĐÃ ĐƯỢC CẤU HÌNH ĐÚNG:**

1. ✅ **Gateway Service**: Gọi tất cả downstream services qua Consul với LoadBalancer
2. ✅ **IAM Service**: Đăng ký với Consul, sẵn sàng nhận requests
3. ✅ **Business Service**: Gọi IAM service qua Consul với LoadBalancer

**Hệ thống đã sẵn sàng cho:**
- ✅ Multiple instances của mỗi service
- ✅ Load balancing tự động
- ✅ High availability
- ✅ Dynamic service discovery

---

**Người kiểm tra**: AI Assistant  
**Ngày**: 2026-01-07  
**Trạng thái**: ✅ **PASSED** - Tất cả requirements đã được đáp ứng

