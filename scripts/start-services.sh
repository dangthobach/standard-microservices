#!/bin/bash
# Script để khởi động các services theo thứ tự
# Usage: ./scripts/start-services.sh

echo "========================================"
echo "Starting Microservices Platform"
echo "========================================"
echo ""

# Bước 1: Infrastructure Layer (Databases, Cache, Message Queue)
echo "[1/5] Starting Infrastructure Layer..."
docker-compose up -d consul postgres redis zookeeper

echo "Waiting for infrastructure services to be healthy..."
sleep 10

# Bước 2: Kafka (phụ thuộc Zookeeper)
echo "[2/5] Starting Kafka..."
docker-compose up -d kafka

echo "Waiting for Kafka to be healthy..."
sleep 15

# Bước 3: Keycloak (phụ thuộc PostgreSQL)
echo "[3/5] Starting Keycloak..."
docker-compose up -d keycloak

echo "Waiting for Keycloak to be healthy (this may take 2-3 minutes)..."
sleep 30

# Bước 4: Observability Stack
echo "[4/5] Starting Observability Stack..."
docker-compose up -d zipkin prometheus grafana

echo "Waiting for observability services..."
sleep 10

# Bước 5: Application Services
echo "[5/5] Starting Application Services..."
echo "  - Starting IAM Service (will run Flyway migrations)..."
docker-compose up -d iam-service

echo "  Waiting for IAM Service to initialize database schema..."
sleep 20

echo "  - Starting Gateway Service..."
docker-compose up -d gateway-service

echo "  - Starting Business Service..."
docker-compose up -d business-service

echo ""
echo "========================================"
echo "All services started!"
echo "========================================"
echo ""

# Kiểm tra trạng thái
echo "Checking service status..."
docker-compose ps

echo ""
echo "Service URLs:"
echo "  - Gateway:      http://localhost:8080"
echo "  - IAM Service:  http://localhost:8081"
echo "  - Business:     http://localhost:8082"
echo "  - Keycloak:     http://localhost:8180"
echo "  - Consul UI:    http://localhost:8500"
echo "  - Grafana:      http://localhost:3000 (admin/admin)"
echo "  - Prometheus:   http://localhost:9090"
echo "  - Zipkin:       http://localhost:9411"
echo ""

# Kiểm tra health của application services
echo "Checking application service health..."
sleep 10

services=("iam-service" "gateway-service" "business-service")
for service in "${services[@]}"; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null)
    if [ "$status" = "healthy" ]; then
        echo "  ✓ $service is healthy"
    elif [ "$status" = "starting" ]; then
        echo "  ⏳ $service is starting..."
    else
        echo "  ✗ $service status: $status"
        echo "    Check logs: docker logs $service"
    fi
done

echo ""
echo "To view logs: docker-compose logs -f [service-name]"
echo "To stop all:  docker-compose down"



