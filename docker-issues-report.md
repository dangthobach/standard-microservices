# Báo Cáo Đánh Giá Issues Docker Services

## Tổng Quan
Có **9 infrastructure services** đang chạy (tất cả đều ở trạng thái "healthy"), nhưng **KHÔNG có application services nào đang chạy** (iam-service, gateway-service, business-service, etc.). Đây là nguyên nhân chính của một số vấn đề.

---

## 🔴 CRITICAL ISSUES (Cần xử lý ngay)

### 1. **PostgreSQL - Database Schema Chưa Được Khởi Tạo**
**Mức độ:** 🔴 CRITICAL

**Vấn đề:**
```
ERROR: relation "permissions" does not exist
ERROR: relation "roles" does not exist  
ERROR: relation "users" does not exist
```

**Nguyên nhân:** 
- Database schema chưa được tạo bởi Flyway migrations
- Có process đang cố gắng tạo indexes trên các bảng chưa tồn tại
- Flyway migration file tồn tại tại: `iam-service/src/main/resources/db/migration/V1__Initial_Schema.sql`
- IAM Service có thể chưa được khởi động hoặc Flyway chưa chạy migrations

**Giải pháp:**
1. **IAM Service hiện tại KHÔNG đang chạy** - Đây là nguyên nhân chính!

2. **Khởi động IAM Service để Flyway chạy migrations:**
   ```bash
   docker-compose up -d iam-service
   ```
   
   Flyway sẽ tự động chạy migration `V1__Initial_Schema.sql` khi service khởi động và tạo các bảng cần thiết.

3. **Hoặc chạy migration thủ công:**
   ```bash
   docker exec -i postgres psql -U postgres -d enterprise_db -c "SET search_path TO iam;"
   # Sau đó chạy migration file
   ```

4. **Kiểm tra schema đã được tạo:**
   ```bash
   docker exec postgres psql -U postgres -d enterprise_db -c "\dt iam.*"
   ```

---

### 2. **Kafka - Invalid Receive Size Exception**
**Mức độ:** 🔴 CRITICAL

**Vấn đề:**
```
InvalidReceiveException: Invalid receive (size = 1195725856 larger than 104857600)
```

**Nguyên nhân:** 
- **ROOT CAUSE**: Prometheus đang cố gắng scrape metrics từ `kafka:9092` (Kafka broker port - binary protocol)
- Prometheus gửi HTTP requests đến port binary protocol, gây ra protocol mismatch
- File config: `infrastructure/prometheus/prometheus.yml` line 92-97

**Giải pháp:**
- **Option 1 (Khuyến nghị)**: Comment out hoặc xóa Kafka scrape config cho đến khi có Kafka Exporter
- **Option 2**: Cài đặt Kafka Exporter và cấu hình scrape từ exporter port (thường là 9308)
- **Option 3**: Nếu Kafka có JMX metrics, sử dụng JMX Exporter

---

## 🟡 WARNINGS (Cần theo dõi)

### 3. **Redis - Security Attack Detection**
**Mức độ:** 🟡 WARNING

**Vấn đề:**
```
Possible SECURITY ATTACK detected. It looks like somebody is sending POST or Host: commands to Redis.
Connection from 172.18.0.7 aborted.
```

**Nguyên nhân:**
- **ROOT CAUSE**: Prometheus (IP: 172.18.0.7) đang cố gắng scrape metrics từ `redis:6379` (Redis data port)
- Prometheus gửi HTTP requests đến Redis protocol port, Redis phát hiện HTTP commands và từ chối
- File config: `infrastructure/prometheus/prometheus.yml` line 84-89

**Giải pháp:**
- **Option 1 (Khuyến nghị)**: Comment out hoặc xóa Redis scrape config cho đến khi có Redis Exporter
- **Option 2**: Cài đặt Redis Exporter (redis_exporter) và cấu hình scrape từ exporter port (thường là 9121)
- **Option 3**: Nếu Redis có INFO command metrics, có thể sử dụng custom exporter

---

### 4. **Keycloak - Development Mode & Resource Leaks**
**Mức độ:** 🟡 WARNING

**Vấn đề:**
```
WARN: Running the server in development mode. DO NOT use this configuration in production.
WARN: Datasource '<default>': JDBC resources leaked: 3 ResultSet(s) and 0 Statement(s)
```

**Giải pháp:**
- Chuyển sang production mode nếu đang deploy production
- Kiểm tra và fix resource leaks trong code

---

### 5. **Consul - Autopilot Reconciliation Failure**
**Mức độ:** 🟡 WARNING

**Vấn đề:**
```
ERROR: agent.server.autopilot: Failed to reconcile current state with the desired state
```

**Nguyên nhân:** Consul autopilot không thể reconcile state (có thể do single node setup)

**Giải pháp:**
- Nếu đang chạy single node, có thể bỏ qua warning này
- Nếu cần cluster, thêm các Consul nodes khác

---

### 6. **Grafana - Missing Provisioning Directories**
**Mức độ:** 🟡 WARNING

**Vấn đề:**
```
ERROR: Failed to read plugin provisioning files from directory: /etc/grafana/provisioning/plugins
ERROR: can't read alerting provisioning files from directory: /etc/grafana/provisioning/alerting
```

**Giải pháp:**
- Tạo các thư mục này nếu cần provisioning
- Hoặc bỏ qua nếu không sử dụng provisioning

---

### 7. **Zookeeper - Client Connection Issues**
**Mức độ:** 🟡 INFO (Có thể bình thường)

**Vấn đề:**
```
Unable to read additional data from client, it probably closed the socket
```

**Nguyên nhân:** Clients đóng kết nối sớm (có thể là health checks)

**Giải pháp:**
- Nếu là health checks, đây là hành vi bình thường
- Nếu là application clients, kiểm tra connection pooling

---

## ✅ Services Hoạt Động Tốt

- **Prometheus**: Không có lỗi, hoạt động bình thường
- **Zipkin**: Chỉ có warning nhỏ về MeterFilter, không ảnh hưởng

---

## 📋 Khuyến Nghị Hành Động

### Ưu tiên cao:
1. ✅ **Fix PostgreSQL schema** - Chạy database migrations
2. ✅ **Fix Prometheus config** - Comment out hoặc sửa Redis và Kafka scrape configs
3. ✅ **Cài đặt Exporters** - Thêm Redis Exporter và Kafka Exporter nếu cần metrics

### Ưu tiên trung bình:
4. ⚠️ **Keycloak production mode** - Chuyển sang production nếu cần
5. ⚠️ **Consul autopilot** - Xem xét nếu cần cluster

### Ưu tiên thấp:
6. ℹ️ **Grafana provisioning** - Tạo thư mục nếu cần
7. ℹ️ **Zookeeper connections** - Monitor nếu có vấn đề thực sự

---

## 🔍 Để Kiểm Tra Thêm

1. Kiểm tra service nào có IP `172.18.0.7` (đang gây vấn đề với Kafka và Redis):
   ```bash
   docker inspect <container> | grep IPAddress
   ```

2. Kiểm tra database migration status:
   ```bash
   docker exec postgres psql -U <user> -d <database> -c "\dt"
   ```

3. Kiểm tra Kafka configuration:
   ```bash
   docker exec kafka-broker cat /etc/kafka/server.properties | grep max.bytes
   ```

4. **Restart Prometheus để áp dụng config mới:**
   ```bash
   docker restart prometheus
   ```

5. **Kiểm tra IAM Service logs để xem Flyway migrations:**
   ```bash
   docker logs iam-service | grep -i flyway
   ```

