# Sitemap — standard-microservices

Mục tiêu của sitemap là giúp “đi tới đúng chỗ” nhanh: docs, services, entrypoints, cấu hình, infra.

## A) Start Here

- `README.md` — overview + quick start + stack
- `DEPLOYMENT_GUIDE.md` — chạy local/docker/k8s
- `docs/ONBOARDING.md` — onboarding dev
- `docs/WIKI.md` — wiki/index kiến trúc (file này)

## B) Maven Modules (Backend)

- `pom.xml` (parent reactor)
- `common-lib/`
- `gateway-service/`
- `iam-service/`
- `business-service/`
- `process-management-service/`
- `integration-service/`

## C) Common Library (Shared Foundation)

- `common-lib/pom.xml`
- Source: `common-lib/src/main/java/com/enterprise/common/`
  - `cqrs/` — CQRS infra (Command/Query/Buses/Handlers)
  - `config/` — global exception/response advice, CQRS registration
  - `dto/` — `ApiResponse`, paging DTOs, error details
  - `entity/` — base entities + state/history primitives
  - `repository/` — base repositories (soft delete, history)
  - `health/`, `metrics/`, `util/`, `audit/`

## D) Gateway Service (Reactive BFF)

- Entrypoint: `gateway-service/src/main/java/com/enterprise/gateway/GatewayApplication.java`
- Config: `gateway-service/src/main/resources/application.yml`
- Source: `gateway-service/src/main/java/com/enterprise/gateway/`
  - `controller/` — auth + dashboard APIs
  - `filter/` — gateway filters (security/tracing/etc.)
  - `security/` — OAuth2/BFF security components
  - `query/` — CQRS queries + handlers cho dashboard metrics
  - `health/` — downstream health logic

## E) IAM Service

- Entrypoint: `iam-service/src/main/java/com/enterprise/iam/IamServiceApplication.java`
- Config: `iam-service/src/main/resources/application.yml`
- Source: `iam-service/src/main/java/com/enterprise/iam/`
  - `controller/`
  - `command/` + `query/` (CQRS)
  - `entity/` + `repository/`
  - `client/` (Feign)
  - `service/`

## F) Business Service

- Entrypoint: `business-service/src/main/java/com/enterprise/business/BusinessServiceApplication.java`
- Config: `business-service/src/main/resources/application.yml`
- Source: `business-service/src/main/java/com/enterprise/business/`
  - `controller/`
  - `command/` + `query/` (CQRS)
  - `entity/` + `repository/` (+ history)
  - `consumer/` (message consumers)
  - `service/` (cross-cutting / integration points)
  - `aspect/` (audit/aspects)

## G) Process Management Service (Workflow)

- Entrypoint: `process-management-service/src/main/java/com/enterprise/process/ProcessServiceApplication.java`
- Config: `process-management-service/src/main/resources/application.yml`
- Source: `process-management-service/src/main/java/com/enterprise/process/`
  - `controller/` — task/process/deployment/analytics APIs
  - `bpmn/`, `dmn/`, `cmmn/` — workflow/decision/case components
  - `consumer/`, `producer/` — messaging integration
  - `integration/` — connector abstractions
  - `websocket/` — realtime updates (nếu có)

## H) Integration Service

- Entrypoint: `integration-service/src/main/java/com/enterprise/integration/IntegrationServiceApplication.java`
- Config: `integration-service/src/main/resources/application.yml`
- Source: `integration-service/src/main/java/com/enterprise/integration/`
  - `controller/` — webhooks/connectors APIs
  - `connector/` — connector implementations/factory
  - `entity/` + `repository/`
  - `service/` + `client/`

## I) Infrastructure & Operations

- Docker:
  - `docker-compose.yml`
  - `docker-compose.workflow.yml`
  - `.env.example`
- Kubernetes:
  - `k8s/00-namespace.yaml`
  - `k8s/01-configmaps.yaml`
  - `k8s/02-secrets.yaml`
  - `k8s/03-business-service.yaml`
  - `k8s/04-iam-service.yaml`
  - `k8s/05-process-service.yaml`
  - `k8s/06-gateway-service.yaml`
  - `k8s/07-ingress.yaml`
  - `k8s/08-integration-service.yaml`
  - `k8s/base/` + `k8s/overlays/`
- Monitoring/Observability:
  - `monitoring/`
  - `infrastructure/prometheus/`
  - `infrastructure/grafana/`
  - `infrastructure/MONITORING_WALKTHROUGH.md`
- CI/CD:
  - `Jenkinsfile`
  - `.github/`
  - `cicd/` (+ ArgoCD manifests)

## J) Database

- `database/README.md`
- `database/init-schemas.sql`
- `database/setup-database.sh`
- `database/DATABASE_SETUP_FIX.md`

## K) Frontend / UI

- Angular app: `frontend/` (see `frontend/README.md`)
- React admin: `admin-ui/` (see `admin-ui/README.md`)

## L) Testing & Tooling

- E2E docs: `E2E_TESTING_GUIDE.md`
- HTTP collection: `e2e-product-approval-test.http`
- Performance tests: `performance-tests/`
- Scripts: `scripts/`

