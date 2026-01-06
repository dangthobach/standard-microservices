# Hướng Dẫn Khởi Động Services Theo Thứ Tự

## Tổng Quan

Hệ thống microservices cần được khởi động theo thứ tự để đảm bảo các dependencies sẵn sàng trước khi service phụ thuộc khởi động.

## Thứ Tự Khởi Động

```
1. Infrastructure Layer
   ├── Consul (Service Discovery)
   ├── PostgreSQL (Database)
   ├── Redis (Cache)
   └── Zookeeper (Kafka dependency)

2. Kafka (phụ thuộc Zookeeper)

3. Keycloak (phụ thuộc PostgreSQL)

4. Observability Stack
   ├── Zipkin (Tracing)
   ├── Prometheus (Metrics)
   └── Grafana (Dashboards)

5. Application Services
   ├── IAM Service (tạo database schema via Flyway)
   ├── Gateway Service
   └── Business Service
```

## Cách 1: Sử Dụng Script Tự Động (Khuyến Nghị)

### Windows (PowerShell)
```powershell
.\scripts\start-services.ps1
```

### Linux/Mac (Bash)
```bash
chmod +x scripts/start-services.sh
./scripts/start-services.sh
```

Script sẽ tự động:
- ✅ Khởi động services theo thứ tự
- ✅ Đợi services healthy trước khi khởi động service tiếp theo
- ✅ Hiển thị trạng thái và URLs của các services

## Cách 2: Khởi Động Thủ Công Theo Từng Bước

### Bước 1: Infrastructure Layer
```bash
docker-compose up -d consul postgres redis zookeeper
```

Đợi 10-15 giây để services khởi động:
```bash
docker-compose ps
```

### Bước 2: Kafka
```bash
docker-compose up -d kafka
```

Đợi 15-20 giây:
```bash
docker logs kafka-broker | grep -i "started"
```

### Bước 3: Keycloak
```bash
docker-compose up -d keycloak
```

Keycloak cần 2-3 phút để khởi động hoàn toàn:
```bash
docker logs keycloak | grep -i "started"
```

### Bước 4: Observability Stack
```bash
docker-compose up -d zipkin prometheus grafana
```

### Bước 5: Application Services

**Quan trọng:** IAM Service phải khởi động trước để chạy Flyway migrations tạo database schema.

```bash
# IAM Service (tạo database schema)
docker-compose up -d iam-service

# Đợi 20-30 giây để Flyway migrations hoàn tất
docker logs iam-service | grep -i "flyway\|migration"

# Gateway Service
docker-compose up -d gateway-service

# Business Service
docker-compose up -d business-service
```

## Cách 3: Sử Dụng Docker Compose Dependencies

Docker Compose đã được cấu hình với `depends_on` và `condition: service_healthy`, nhưng để đảm bảo thứ tự hoàn hảo, bạn có thể khởi động từng nhóm:

```bash
# Nhóm 1: Infrastructure cơ bản
docker-compose up -d consul postgres redis zookeeper

# Nhóm 2: Kafka
docker-compose up -d kafka

# Nhóm 3: Keycloak
docker-compose up -d keycloak

# Nhóm 4: Observability
docker-compose up -d zipkin prometheus grafana

# Nhóm 5: Application services (sẽ tự động đợi dependencies)
docker-compose up -d iam-service gateway-service business-service
```

## Kiểm Tra Trạng Thái Services

### Xem tất cả services
```bash
docker-compose ps
```

### Kiểm tra health status
```bash
# Windows PowerShell
docker inspect --format='{{.Name}}: {{.State.Health.Status}}' $(docker ps -q)

# Linux/Mac
docker inspect --format='{{.Name}}: {{.State.Health.Status}}' $(docker ps -q)
```

### Xem logs của service cụ thể
```bash
docker-compose logs -f iam-service
docker-compose logs -f gateway-service
docker-compose logs -f business-service
```

### Kiểm tra database schema đã được tạo
```bash
docker exec postgres psql -U postgres -d enterprise_db -c "\dt iam.*"
```

## Xử Lý Lỗi

### Service không khởi động được

1. **Kiểm tra logs:**
   ```bash
   docker-compose logs [service-name]
   ```

2. **Kiểm tra dependencies:**
   - Đảm bảo tất cả dependencies đã healthy
   - Ví dụ: IAM Service cần PostgreSQL healthy

3. **Restart service:**
   ```bash
   docker-compose restart [service-name]
   ```

### Database schema chưa được tạo

Nếu IAM Service đã chạy nhưng schema chưa có:

1. **Kiểm tra Flyway logs:**
   ```bash
   docker logs iam-service | grep -i flyway
   ```

2. **Chạy migration thủ công (nếu cần):**
   ```bash
   docker exec -i postgres psql -U postgres -d enterprise_db < iam-service/src/main/resources/db/migration/V1__Initial_Schema.sql
   ```

### Service không healthy

1. **Kiểm tra healthcheck:**
   ```bash
   docker inspect [service-name] | grep -A 10 Healthcheck
   ```

2. **Test health endpoint thủ công:**
   ```bash
   curl http://localhost:8081/actuator/health  # IAM Service
   curl http://localhost:8080/actuator/health  # Gateway
   ```

## Dừng Services

### Dừng tất cả
```bash
docker-compose down
```

### Dừng và xóa volumes (reset hoàn toàn)
```bash
docker-compose down -v
```

### Dừng từng service
```bash
docker-compose stop iam-service
docker-compose stop gateway-service
```

## Service URLs

Sau khi khởi động thành công, các services có thể truy cập tại:

| Service | URL | Credentials |
|---------|-----|-------------|
| Gateway | http://localhost:8080 | - |
| IAM Service | http://localhost:8081 | - |
| Business Service | http://localhost:8082 | - |
| Keycloak | http://localhost:8180 | admin/admin |
| Consul UI | http://localhost:8500 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Zipkin | http://localhost:9411 | - |

## Tips

1. **Sử dụng script tự động** để tránh lỗi thứ tự khởi động
2. **Kiểm tra logs** nếu service không healthy
3. **Đợi đủ thời gian** cho Keycloak (2-3 phút) và IAM Service (20-30 giây cho migrations)
4. **Sử dụng `docker-compose ps`** để theo dõi trạng thái real-time



