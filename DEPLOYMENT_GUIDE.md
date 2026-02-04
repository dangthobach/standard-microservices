# Deployment Guide - Product Management System

## Prerequisites

- Docker & Docker Compose installed
- Kubernetes cluster (for production)
- kubectl configured
- 8GB+ RAM for local development
- 16GB+ RAM for full stack

---

## Local Development Deployment

### 1. Start Infrastructure Only

```bash
# Start minimal infrastructure for development
docker-compose -f docker-compose.workflow.yml up -d

# Verify services
docker-compose -f docker-compose.workflow.yml ps

# View logs
docker-compose -f docker-compose.workflow.yml logs -f
```

**Services Started:**
- PostgreSQL (port 5432)
- RabbitMQ (ports 5672, 15672)
- Redis (port 6379)
- Prometheus (port 9090)
- Grafana (port 3001)

### 2. Run Services Locally

```bash
# Terminal 1: IAM Service
cd iam-service
mvn spring-boot:run -Dspring.profiles.active=local

# Terminal 2: Business Service  
cd business-service
mvn spring-boot:run -Dspring.profiles.active=local

# Terminal 3: Process Service
cd process-management-service
mvn spring-boot:run -Dspring.profiles.active=local

# Terminal 4: Gateway (optional)
cd gateway-service
mvn spring-boot:run -Dspring.profiles.active=local
```

### 3. Run Flowable UI

```bash
cd process-management-service/flowable-ui
npm install
npm run dev
```

**Access URLs:**
- Business Service: http://localhost:8081
- IAM Service: http://localhost:8082
- Process Service: http://localhost:8083
- Gateway: http://localhost:8080
- Flowable UI: http://localhost:3000
- RabbitMQ UI: http://localhost:15672 (guest/guest)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)

---

## Full Stack Docker Deployment

### Option 1: Use Existing docker-compose.yml

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs for specific service
docker-compose logs -f business-service

# Stop all
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Option 2: Use Workflow-Specific Stack

```bash
# Build services first
mvn clean package -DskipTests

# Start workflow stack
docker-compose -f docker-compose.workflow.yml up -d

# Scale a service
docker-compose -f docker-compose.workflow.yml up -d --scale business-service=3
```

---

## Production Kubernetes Deployment

### 1. Prepare Environment

```bash
# Set context to production cluster
kubectl config use-context production-cluster

# Create namespace
kubectl apply -f k8s/00-namespace.yaml

# Verify namespace
kubectl get namespaces | grep product-mgmt
```

### 2. Configure Secrets

```bash
# Update secrets with production values
# IMPORTANT: Change all passwords before applying!

# Edit secrets file
vi k8s/02-secrets.yaml

# Apply secrets
kubectl apply -f k8s/02-secrets.yaml -n product-mgmt-prod

# Verify
kubectl get secrets -n product-mgmt-prod
```

### 3. Deploy Infrastructure Components

```bash
# PostgreSQL (or use managed database)
# RabbitMQ (or use managed service)
# Redis (or use managed service)

# For managed services, update connection strings in ConfigMaps
kubectl apply -f k8s/01-configmaps.yaml -n product-mgmt-prod
```

### 4. Deploy Application Services

```bash
# Deploy in order (dependencies first)
kubectl apply -f k8s/03-business-service.yaml -n product-mgmt-prod
kubectl apply -f k8s/04-iam-service.yaml -n product-mgmt-prod
kubectl apply -f k8s/05-process-service.yaml -n product-mgmt-prod
kubectl apply -f k8s/06-gateway-service.yaml -n product-mgmt-prod

# Wait for rollout
kubectl rollout status deployment/business-service -n product-mgmt-prod
kubectl rollout status deployment/iam-service -n product-mgmt-prod
kubectl rollout status deployment/process-service -n product-mgmt-prod
kubectl rollout status deployment/gateway-service -n product-mgmt-prod
```

### 5. Configure Ingress

```bash
# Install Nginx Ingress Controller (if not installed)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Update domain in ingress.yaml
vi k8s/07-ingress.yaml

# Apply ingress
kubectl apply -f k8s/07-ingress.yaml -n product-mgmt-prod

# Get ingress IP
kubectl get ingress -n product-mgmt-prod
```

### 6. Verify Deployment

```bash
# Check all pods
kubectl get pods -n product-mgmt-prod

# Check services
kubectl get svc -n product-mgmt-prod

# Check HPA
kubectl get hpa -n product-mgmt-prod

# View logs
kubectl logs -f deployment/business-service -n product-mgmt-prod
```

### 7. Run Health Checks

```bash
# Get gateway service URL
GATEWAY_URL=$(kubectl get svc gateway-service -n product-mgmt-prod -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Test health endpoints
curl http://$GATEWAY_URL:8080/actuator/health
curl http://api.example.com/actuator/health
```

---

## CI/CD Deployment (GitHub Actions)

### 1. Configure Secrets

In GitHub repository settings, add:

**Required Secrets:**
- `KUBE_CONFIG_STAGING` - Kubernetes config for staging
- `KUBE_CONFIG_PROD` - Kubernetes config for production
- `SNYK_TOKEN` - Snyk API token for security scanning
- `SLACK_WEBHOOK` - Slack webhook for notifications
- `GITHUB_TOKEN` - Auto-provided by GitHub

### 2. Trigger Deployment

```bash
# Push to develop branch → Deploy to staging
git checkout develop
git push origin develop

# Push to main branch → Deploy to production (with approval)
git checkout main
git merge develop
git push origin main
```

### 3. Monitor Deployment

1. Go to GitHub Actions tab
2. Watch workflow progress
3. Approve production deployment when ready
4. Monitor post-deployment health

---

## Database Migrations

### Local/Docker

Flyway migrations run automatically on service startup.

### Kubernetes Production

```bash
# Option 1: Run via init container (recommended)
# Already configured in deployment manifests

# Option 2: Manual migration
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  mvn flyway:migrate -Dflyway.url=$DB_URL

# Verify migration status
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  mvn flyway:info
```

---

## Monitoring Setup

### Access Monitoring Tools

**Local Development:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

**Production:**
- Prometheus: https://monitoring.example.com/prometheus
- Grafana: https://monitoring.example.com/grafana

### Import Dashboards

1. Login to Grafana (admin/admin)
2. Go to Dashboards → Import
3. Upload JSON files from `monitoring/grafana/dashboards/`
4. Or auto-provision via ConfigMap

### Key Metrics to Monitor

- **Application Metrics:**
  - Request rate (requests/sec)
  - Error rate (%)
  - Response time (p50, p95, p99)
  - Active sessions

- **Business Metrics:**
  - Products created/hour
  - Approval rate (%)
  - Average approval time
  - Workflow completion rate

- **Infrastructure Metrics:**
  - CPU usage
  - Memory usage
  - Database connections
  - RabbitMQ queue depth
  - Redis cache hit rate

---

## Troubleshooting

### Services Won't Start

```bash
# Check logs
kubectl logs deployment/business-service -n product-mgmt-prod --tail=100

# Common issues:
# 1. Database connection failed
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  nc -zv postgres-service 5432

# 2. RabbitMQ connection failed
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  nc -zv rabbitmq-service 5672

# 3. Environment variables
kubectl exec -it deployment/business-service -n product-mgmt-prod -- env | grep DB
```

### Pod CrashLoopBackOff

```bash
# Get pod name
kubectl get pods -n product-mgmt-prod

# Describe pod
kubectl describe pod <pod-name> -n product-mgmt-prod

# Check previous logs
kubectl logs <pod-name> -n product-mgmt-prod --previous
```

### Ingress Not Working

```bash
# Check ingress status
kubectl describe ingress product-mgmt-ingress -n product-mgmt-prod

# Check ingress controller
kubectl get pods -n ingress-nginx

# Test internal service
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://gateway-service.product-mgmt-prod.svc.cluster.local:8080/actuator/health
```

### Database Migration Failed

```bash
# Check Flyway schema history
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  psql -h postgres-service -U postgres -d business_db -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Repair migration
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  mvn flyway:repair
```

---

## Rollback Procedures

### Kubernetes Rollback

```bash
# View rollout history
kubectl rollout history deployment/business-service -n product-mgmt-prod

# Rollback to previous version
kubectl rollout undo deployment/business-service -n product-mgmt-prod

# Rollback to specific revision
kubectl rollout undo deployment/business-service -n product-mgmt-prod --to-revision=2

# Monitor rollback
kubectl rollout status deployment/business-service -n product-mgmt-prod
```

### Database Rollback

```bash
# Rollback last migration
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  mvn flyway:undo

# Or restore from backup
# (Ensure you have database backups!)
```

---

## Scaling

### Manual Scaling

```bash
# Scale deployment
kubectl scale deployment business-service --replicas=5 -n product-mgmt-prod

# Verify
kubectl get deployment business-service -n product-mgmt-prod
```

### Auto-Scaling (HPA)

```bash
# Check HPA status
kubectl get hpa -n product-mgmt-prod

# Update HPA min/max replicas
kubectl patch hpa business-service-hpa -n product-mgmt-prod \
  -p '{"spec":{"minReplicas":5,"maxReplicas":20}}'
```

---

## Backup & Restore

### Database Backup

```bash
# Backup
kubectl exec -it postgres-service-pod -n product-mgmt-prod -- \
  pg_dump -U postgres business_db > backup-$(date +%Y%m%d).sql

# Restore
kubectl exec -i postgres-service-pod -n product-mgmt-prod -- \
  psql -U postgres business_db < backup-20260204.sql
```

### RabbitMQ Backup

```bash
# Export definitions
curl -u guest:guest http://rabbitmq:15672/api/definitions > rabbitmq-backup.json

# Import definitions
curl -u guest:guest -X POST -H "Content-Type: application/json" \
  -d @rabbitmq-backup.json http://rabbitmq:15672/api/definitions
```

---

## Security Hardening

### 1. Update Secrets

```bash
# Generate strong passwords
openssl rand -base64 32

# Update secrets
kubectl edit secret postgres-credentials -n product-mgmt-prod
kubectl edit secret rabbitmq-credentials -n product-mgmt-prod
kubectl edit secret redis-credentials -n product-mgmt-prod
```

### 2. Enable Network Policies

Network policies are already defined in `07-ingress.yaml`. Ensure they're applied:

```bash
kubectl get networkpolicies -n product-mgmt-prod
```

### 3. Regular Security Scans

```bash
# Scan images
trivy image your-registry/business-service:latest

# Scan Kubernetes manifests
kubesec scan k8s/03-business-service.yaml
```

---

## Performance Tuning

### JVM Options

Already configured in Dockerfiles:
```
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0
```

### Database Connection Pool

Configured in ConfigMaps:
```yaml
hikari:
  maximum-pool-size: 50
  minimum-idle: 10
```

### RabbitMQ

```yaml
prefetch-count: 20
concurrent-consumers: 5
max-concurrent-consumers: 20
```

---

## Support & Contacts

- **Issues:** GitHub Issues
- **Slack:** #product-mgmt-ops
- **On-Call:** PagerDuty rotation
- **Documentation:** Confluence Wiki
