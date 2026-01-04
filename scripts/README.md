# Startup Scripts

Scripts để khởi động các services theo thứ tự đúng.

## Sử Dụng

### Windows
```powershell
.\scripts\start-services.ps1
```

### Linux/Mac
```bash
chmod +x scripts/start-services.sh
./scripts/start-services.sh
```

## Thứ Tự Khởi Động

1. **Infrastructure**: Consul, PostgreSQL, Redis, Zookeeper
2. **Kafka** (phụ thuộc Zookeeper)
3. **Keycloak** (phụ thuộc PostgreSQL)
4. **Observability**: Zipkin, Prometheus, Grafana
5. **Application Services**: IAM Service → Gateway Service → Business Service

## Lưu Ý

- IAM Service phải khởi động trước để chạy Flyway migrations tạo database schema
- Keycloak cần 2-3 phút để khởi động hoàn toàn
- Script sẽ tự động đợi services healthy trước khi khởi động service tiếp theo

Xem chi tiết tại: [SERVICE_STARTUP_GUIDE.md](../SERVICE_STARTUP_GUIDE.md)


