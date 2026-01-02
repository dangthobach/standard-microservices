# Script để khởi động các services theo thứ tự
# Usage: .\scripts\start-services.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Microservices Platform" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Bước 1: Infrastructure Layer (Databases, Cache, Message Queue)
Write-Host "[1/5] Starting Infrastructure Layer..." -ForegroundColor Yellow
docker-compose up -d consul postgres redis zookeeper

Write-Host "Waiting for infrastructure services to be healthy..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# Bước 2: Kafka (phụ thuộc Zookeeper)
Write-Host "[2/5] Starting Kafka..." -ForegroundColor Yellow
docker-compose up -d kafka

Write-Host "Waiting for Kafka to be healthy..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Bước 3: Keycloak (phụ thuộc PostgreSQL)
Write-Host "[3/5] Starting Keycloak..." -ForegroundColor Yellow
docker-compose up -d keycloak

Write-Host "Waiting for Keycloak to be healthy (this may take 2-3 minutes)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Bước 4: Observability Stack
Write-Host "[4/5] Starting Observability Stack..." -ForegroundColor Yellow
docker-compose up -d zipkin prometheus grafana

Write-Host "Waiting for observability services..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# Bước 5: Application Services
Write-Host "[5/5] Starting Application Services..." -ForegroundColor Yellow
Write-Host "  - Starting IAM Service (will run Flyway migrations)..." -ForegroundColor Gray
docker-compose up -d iam-service

Write-Host "  Waiting for IAM Service to initialize database schema..." -ForegroundColor Gray
Start-Sleep -Seconds 20

Write-Host "  - Starting Gateway Service..." -ForegroundColor Gray
docker-compose up -d gateway-service

Write-Host "  - Starting Business Service..." -ForegroundColor Gray
docker-compose up -d business-service

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "All services started!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Kiểm tra trạng thái
Write-Host "Checking service status..." -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "  - Gateway:      http://localhost:8080" -ForegroundColor White
Write-Host "  - IAM Service:  http://localhost:8081" -ForegroundColor White
Write-Host "  - Business:     http://localhost:8082" -ForegroundColor White
Write-Host "  - Keycloak:     http://localhost:8180" -ForegroundColor White
Write-Host "  - Consul UI:    http://localhost:8500" -ForegroundColor White
Write-Host "  - Grafana:      http://localhost:3000 (admin/admin)" -ForegroundColor White
Write-Host "  - Prometheus:   http://localhost:9090" -ForegroundColor White
Write-Host "  - Zipkin:       http://localhost:9411" -ForegroundColor White
Write-Host ""

# Kiểm tra health của application services
Write-Host "Checking application service health..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

$services = @("iam-service", "gateway-service", "business-service")
foreach ($service in $services) {
    $status = docker inspect --format='{{.State.Health.Status}}' $service 2>$null
    if ($status -eq "healthy") {
        Write-Host "  ✓ $service is healthy" -ForegroundColor Green
    } elseif ($status -eq "starting") {
        Write-Host "  ⏳ $service is starting..." -ForegroundColor Yellow
    } else {
        Write-Host "  ✗ $service status: $status" -ForegroundColor Red
        Write-Host "    Check logs: docker logs $service" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "To view logs: docker-compose logs -f [service-name]" -ForegroundColor Gray
Write-Host "To stop all:  docker-compose down" -ForegroundColor Gray

