# Project Wiki — Enterprise Microservices Platform

Tài liệu này là “wiki” kiến trúc + index thành phần chính của repo `standard-microservices` theo góc nhìn Solution Architecture.

## 1) Executive Summary

- **Style**: Microservices + API Gateway/BFF, Service Discovery (Consul), CQRS (custom trong `common-lib`), DDD-ish layout theo concern (controller/command/query/entity/repository/config).
- **Runtime**:
  - **Gateway (WebFlux)** làm **single entry point**, thực hiện **OAuth2/OIDC (Keycloak)**, session/BFF, routing `lb://{service}` qua Consul.
  - Downstream services (Servlet/Virtual Threads) validate JWT (Resource Server) và chạy business logic.
- **Core platform libs**: `common-lib` cung cấp CQRS infrastructure, response wrapper chuẩn hoá (`ApiResponse`), tracing enrichment, exception handling, base entities/auditing.

## 2) Modules (Maven Reactor)

Root `pom.xml` định nghĩa modules:

- `common-lib`
- `gateway-service`
- `iam-service`
- `business-service`
- `process-management-service`
- `integration-service`

## 3) System Topology (Local Ports & Entry Points)

Theo cấu hình `application.yml`:

- **gateway-service**: `:8080`
- **iam-service**: `:8081`
- **business-service**: `:8082`
- **process-management-service**: `:8083`
- **integration-service**: `:8084`

Các endpoints observability (mặc định Spring): `/actuator/health`, `/actuator/prometheus`, ...

## 4) API Gateway Pattern (Routing)

Gateway routes (Spring Cloud Gateway) map theo prefix:

- `/api/iam/**` → `lb://iam-service` (rewrite về `/api/{segment}`)
- `/api/business/**` → `lb://business-service`
- `/api/process/**` → `lb://process-service`
- `/api/integration/**` → `lb://integration-service`

File tham chiếu: `gateway-service/src/main/resources/application.yml`.

## 5) Security Model (BFF + OAuth2/OIDC)

Hệ thống áp dụng BFF pattern (Gateway giữ token, client giữ session cookie) theo doc:

- `docs/AUTHZ_WORKFLOW.md`

Key points:

- Gateway là **confidential client**, exchange auth code (PKCE) → tokens, lưu session trong Redis (L2) + Caffeine (L1).
- Downstream services là **OAuth2 Resource Server**, validate JWT theo `issuer-uri`/`jwk-set-uri`.

## 6) CQRS Pattern (Custom — Most Critical)

CQRS infrastructure nằm trong `common-lib`:

- Interfaces: `com.enterprise.common.cqrs.Command`, `Query`, `CommandHandler`, `QueryHandler`
- Buses: `InMemoryCommandBus`, `InMemoryQueryBus`
- Auto-discovery: `com.enterprise.common.config.CqrsConfiguration` (đăng ký handler theo generic type sau startup)

Doc tham chiếu: `common-lib/src/main/java/com/enterprise/common/cqrs/README.md`.

### 6.1. Contract / Controller Rule of Thumb

- **Controller** nên inject **`CommandBus`** và **`QueryBus`** (không inject repository/service trực tiếp).
- **Commands**: mutate state, transactional, return IDs (hoặc minimal result).
- **Queries**: read-only, trả DTO, có thể cache.

## 7) API Response + Tracing + Error Handling

Chuẩn hoá response và tracing:

- `com.enterprise.common.dto.ApiResponse`
- `com.enterprise.common.config.GlobalExceptionHandler` (Servlet stack)
- `com.enterprise.common.config.GlobalResponseBodyAdvice` (tự động enrich `traceId`, `spanId`, `requestId`)
- Doc: `docs/API_RESPONSE_PATTERNS.md`

## 8) Data, Persistence, History/Audit

### 8.1. Entity Base Classes

Các base entities trong `common-lib`:

- `com.enterprise.common.entity.BaseEntity`
- `com.enterprise.common.entity.AuditableEntity`
- `com.enterprise.common.entity.SoftDeletableEntity`
- `com.enterprise.common.entity.StatefulEntity`

### 8.2. History Pattern (Explicit History Tables)

Ví dụ history entity hiện có:

- `business-service`: `ProductHistory` (+ repository `ProductHistoryRepository`)
- `iam-service`: `UserRequestHistory` (+ repository `UserRequestHistoryRepository`)
- `common-lib`: `StateHistory` (+ repository `StateHistoryRepository`)

Nguyên tắc implement được mô tả tại `entity_implementation_guide.md`.

## 9) Messaging / Integration

Theo cấu hình:

- **Kafka**: `business-service`, `process-management-service`
- **RabbitMQ**: `business-service`, `process-management-service`
- **Integration**: `integration-service` có khái niệm connectors/webhooks

## 10) Source Index (By Service)

Mục này “index” các package chính + entrypoints + API controllers + CQRS handlers + entities/repositories tiêu biểu.

### 10.1. `common-lib` (Shared Foundation)

- **Root package**: `common-lib/src/main/java/com/enterprise/common/`
- **Key packages**:
  - `cqrs/`: CQRS interfaces + in-memory buses + validation/dispatch
  - `config/`: global exception/response advice, CQRS config, JPA audit config, reactive equivalents
  - `dto/`: `ApiResponse`, paging DTOs, error details
  - `entity/`: base entities (auditing, soft delete, state)
  - `repository/`: base repositories + history repositories
  - `feign/`: shared Feign support (Gateway explicitly excludes)

### 10.2. `gateway-service` (Reactive WebFlux Gateway/BFF)

- **Entrypoint**: `gateway-service/src/main/java/com/enterprise/gateway/GatewayApplication.java`
- **Packages**: `config/`, `controller/`, `filter/`, `security/`, `service/`, `query/`, `health/`
- **Controllers (examples)**:
  - `com.enterprise.gateway.controller.AuthController`
  - `com.enterprise.gateway.controller.DashboardController`
- **Queries/Handlers (dashboard metrics)**:
  - `com.enterprise.gateway.query.*`
  - `com.enterprise.gateway.query.handler.*`

### 10.3. `iam-service` (Identity & Access Management)

- **Entrypoint**: `iam-service/src/main/java/com/enterprise/iam/IamServiceApplication.java`
- **Packages**: `client/`, `command/`, `query/`, `controller/`, `entity/`, `repository/`, `service/`
- **CQRS samples**:
  - Command: `CreateUserCommand` + `CreateUserCommandHandler`
  - Query: `GetUserByIdQuery` + `GetUserByIdQueryHandler`
- **Controllers (examples)**:
  - `UserController`, `UserRequestController`, `ClientAuthController`, `internal/InternalAuthZController`

### 10.4. `business-service` (Core Business)

- **Entrypoint**: `business-service/src/main/java/com/enterprise/business/BusinessServiceApplication.java`
- **Packages**: `command/`, `query/`, `controller/`, `entity/`, `repository/`, `consumer/`, `service/`, `aspect/`, `client/`
- **CQRS handlers**:
  - Commands: `CreateProductCommandHandler`, `UpdateProductCommandHandler`, `DeleteProductCommandHandler`
  - Queries: `GetProductByIdQueryHandler`, `GetProductBySkuQueryHandler`, `ListProductsQueryHandler`
- **Entities/History**:
  - `Product`, `ProductAttachment`, `ProductHistory`
  - `AuditLog`
- **Controllers (examples)**:
  - `ProductController`
  - `ProductFileController`
  - `ProcessRequestController`
  - `RabbitMQMonitoringController`

### 10.5. `process-management-service` (Workflow Orchestration)

- **Entrypoint**: `process-management-service/src/main/java/com/enterprise/process/ProcessServiceApplication.java`
- **Packages**: `bpmn/`, `cmmn/`, `dmn/`, `controller/`, `consumer/`, `producer/`, `service/`, `websocket/`, `integration/`
- **Controllers (examples)**:
  - `TaskController`
  - `DeploymentController`
  - `ProcessDefinitionController`
  - `WorkflowAnalyticsController`
  - `IntegrationController`

### 10.6. `integration-service` (Third-party Integrations)

- **Entrypoint**: `integration-service/src/main/java/com/enterprise/integration/IntegrationServiceApplication.java`
- **Packages**: `connector/`, `controller/`, `entity/`, `repository/`, `service/`, `client/`, `dto/`
- **Controllers (examples)**:
  - `WebhookController`
  - `ConnectorController`
  - `IntegrationController`
- **Entities**:
  - `WebhookEndpoint`, `WebhookEvent`, `ConnectorConfig`, `ConnectorExecution`

## 11) Non-code Areas (Ops/Docs/UI)

- **Docs**: `docs/` (authz workflow, tracing, consul, spa architecture, openfeign usage, workflow advanced features)
- **Kubernetes manifests**: `k8s/` + `k8s/base/` + `k8s/overlays/`
- **Docker compose**: `docker-compose.yml`, `docker-compose.workflow.yml`
- **Monitoring**: `monitoring/`, `infrastructure/prometheus`, `infrastructure/grafana`
- **Database scripts**: `database/` (init schema, setup scripts)
- **Frontends**:
  - Angular: `frontend/`
  - React admin: `admin-ui/`

## 12) Architecture Notes / Conformance Check (CQRS)

Repo có CQRS foundation rất rõ, tuy nhiên vẫn có các điểm “lệch” pattern tại một số controller (ví dụ controller inject repository/service trực tiếp). Nếu mục tiêu là **strict CQRS everywhere**, nên:

- Chuẩn hoá controller theo rule: **Controller → (CommandBus/QueryBus) → Handlers → Repository**.
- Dọn dẹp controller nào còn gọi thẳng repository/service để tránh “leak” business logic ra ngoài handler.

