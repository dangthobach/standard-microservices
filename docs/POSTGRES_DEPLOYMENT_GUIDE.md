# PostgreSQL Deployment Guide (On-Prem / Docker / Kubernetes)

Tài liệu này hướng dẫn triển khai PostgreSQL cho hệ thống theo kiến trúc **single database + schema-per-service** và bao gồm **các bước kiểm tra** để đảm bảo DB/schema khớp với cấu hình hiện tại, tránh exception khi service startup.

## 1) Target Architecture (DB & Schemas)

- **Database**: `enterprise_db`
- **Schemas**:
  - **IAM**: `iam_schema`
  - **Business**: `business_schema`
  - **Integration**: `integration_schema`
  - **Process (Flowable)**: `flowable`

### 1.1. Cấu hình service (nguyên tắc)

Mỗi service dùng chung DB nhưng khác schema thông qua biến môi trường:

- `DB_HOST`, `DB_PORT`, `DB_NAME=enterprise_db`
- `DB_SCHEMA`:
  - `iam_schema` / `business_schema` / `integration_schema` / `flowable`

Trong Spring Boot (đã refactor), các service sẽ:
- JDBC: `.../${DB_NAME}?currentSchema=${DB_SCHEMA}`
- JPA: `hibernate.default_schema=${DB_SCHEMA}`
- Flyway: `default-schema=${DB_SCHEMA}`, `schemas=${DB_SCHEMA}`, `create-schemas=true`

## 2) Checklist kiểm tra trước triển khai (bắt buộc)

> Chạy checklist này trước khi deploy để tránh lỗi kiểu: `relation "..." does not exist`, `schema ... does not exist`, `flyway_schema_history not found`, Flowable tạo bảng sai schema, v.v.

### 2.1. Kiểm tra PostgreSQL đã chạy và reachable

```bash
pg_isready -h <DB_HOST> -p 5432 -U <DB_USER>
```

### 2.2. Kiểm tra database `enterprise_db` đã tồn tại chưa

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d postgres -c "\l"
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d postgres -c "SELECT datname FROM pg_database WHERE datname='enterprise_db';"
```

Nếu chưa có:

```sql
CREATE DATABASE enterprise_db;
```

### 2.3. Kiểm tra schemas có đúng 4 schema mục tiêu không

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c "\dn"
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c "
SELECT nspname
FROM pg_namespace
WHERE nspname IN ('iam_schema','business_schema','integration_schema','flowable')
ORDER BY 1;"
```

Nếu thiếu schema nào, tạo schema tương ứng:

```sql
CREATE SCHEMA IF NOT EXISTS iam_schema;
CREATE SCHEMA IF NOT EXISTS business_schema;
CREATE SCHEMA IF NOT EXISTS integration_schema;
CREATE SCHEMA IF NOT EXISTS flowable;
```

### 2.4. Kiểm tra service đang cấu hình đúng DB/schema chưa (đối chiếu ENV)

Trên môi trường deploy, kiểm tra các biến môi trường:

- `DB_NAME` phải là **`enterprise_db`**
- `DB_SCHEMA` phải tương ứng đúng service:
  - `iam-service` → `iam_schema`
  - `business-service` → `business_schema`
  - `integration-service` → `integration_schema`
  - `process-management-service` → `flowable`

### 2.5. Kiểm tra Flyway đã/đang migrate trong đúng schema chưa

Mỗi schema sẽ có bảng `flyway_schema_history` riêng (vì `default-schema` = schema service).

Ví dụ kiểm tra IAM:

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c \
"SET search_path TO iam_schema; SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 20;"
```

Business:

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c \
"SET search_path TO business_schema; SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 20;"
```

Integration:

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c \
"SET search_path TO integration_schema; SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 20;"
```

Process/Flowable:

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c \
"SET search_path TO flowable; SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 20;"
```

### 2.6. Kiểm tra tables “tối thiểu phải có” theo schema

> Danh sách dưới đây là để sanity-check nhanh. Chi tiết bảng phụ thuộc migration từng service.

IAM (`iam_schema`):

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c "SET search_path TO iam_schema; \dt"
```

Business (`business_schema`):

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c "SET search_path TO business_schema; \dt"
```

Integration (`integration_schema`):

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c "SET search_path TO integration_schema; \dt"
```

Process/Flowable (`flowable`):

```bash
psql -h <DB_HOST> -p 5432 -U <DB_USER> -d enterprise_db -c "SET search_path TO flowable; \dt"
```

## 3) On-Prem PostgreSQL Deployment

### 3.1. Tạo DB + schemas (one-time)

- Tạo `enterprise_db`
- Tạo 4 schema mục tiêu

Bạn có thể tham khảo script:
- `database/init-schemas.sql` (tạo schemas + seed mẫu; lưu ý seed chỉ phù hợp cho dev/demo)

### 3.2. Quyền truy cập (khuyến nghị)

Tối thiểu:
- user deploy/service có quyền `CONNECT` vào `enterprise_db`
- `USAGE, CREATE` trên schema tương ứng
- `SELECT/INSERT/UPDATE/DELETE` trên tables trong schema

## 4) Docker (dev) — PostgreSQL chạy on host / hoặc container

### 4.1. Nếu PostgreSQL chạy on host

Trong `docker-compose.yml`, các service đang dùng:
- `DB_HOST=host.docker.internal`
- `DB_NAME=enterprise_db`
- `DB_SCHEMA=<schema của service>`

Bạn cần đảm bảo host Postgres có `enterprise_db` + schemas.

### 4.2. Nếu muốn chạy PostgreSQL trong Docker

Repo có manifest Postgres trong `k8s/base/postgres-statefulset.yaml` (K8s). Nếu chạy Docker thuần, hãy tạo container postgres và expose 5432, rồi set:
- `DB_HOST=postgres` (service name)
- `DB_NAME=enterprise_db`
- `DB_SCHEMA=...`

## 5) Kubernetes Deployment

### 5.1. Kiểm tra Postgres service/pod đã tồn tại chưa

```bash
kubectl get svc -A | rg "postgres"
kubectl get pods -A | rg "postgres"
```

Nếu dùng Postgres in-cluster:
- manifest tham khảo: `k8s/base/postgres-statefulset.yaml`

### 5.2. Kiểm tra DB_NAME/DB_SCHEMA trong deployments

Ví dụ:

```bash
kubectl -n product-mgmt-prod get deploy iam-service -o yaml | rg "DB_NAME|DB_SCHEMA|DB_HOST"
kubectl -n product-mgmt-prod get deploy business-service -o yaml | rg "DB_NAME|DB_SCHEMA|DB_HOST"
kubectl -n product-mgmt-prod get deploy process-service -o yaml | rg "DB_NAME|DB_SCHEMA|DB_HOST"
kubectl -n product-mgmt-prod get deploy integration-service -o yaml | rg "DB_NAME|DB_SCHEMA|DB_HOST"
```

Kỳ vọng:
- `DB_NAME=enterprise_db`
- `DB_SCHEMA` đúng theo service
- `DB_HOST` trỏ về **cùng 1** postgres service (không tách per-service)

### 5.3. Kiểm tra DB/schema trực tiếp từ cluster (psql in pod)

Nếu bạn có pod postgres:

```bash
kubectl -n <ns> exec -it statefulset/postgres -- psql -U postgres -d enterprise_db -c "\dn"
```

Hoặc tạo ephemeral psql pod:

```bash
kubectl -n <ns> run psql --rm -it --image=postgres:16-alpine --restart=Never -- \
  psql -h postgres-service -U postgres -d enterprise_db -c "\dn"
```

## 6) Troubleshooting nhanh (schema mismatch)

### 6.1. Dấu hiệu thường gặp

- `ERROR: relation "..." does not exist`
- `ERROR: schema "..." does not exist`
- Flyway chạy nhưng tạo bảng ở `public` thay vì schema service
- Flowable tạo bảng ở `public`/schema khác

### 6.2. Checklist fix

- Confirm **`DB_NAME=enterprise_db`**
- Confirm **`DB_SCHEMA`** đúng service
- Confirm JDBC URL có `?currentSchema=${DB_SCHEMA}`
- Confirm Flyway config:
  - `create-schemas=true`
  - `default-schema=${DB_SCHEMA}`
  - `schemas=${DB_SCHEMA}`
- Confirm JPA config:
  - `hibernate.default_schema=${DB_SCHEMA}`
- Với process-service:
  - `flowable.database-schema=${DB_SCHEMA}`

