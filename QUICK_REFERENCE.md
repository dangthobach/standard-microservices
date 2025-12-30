# Quick Reference Guide

**Last Updated**: December 30, 2024

---

## 🚀 Quick Start

### Local Development
```bash
# Start infrastructure
docker-compose up -d postgres redis kafka zookeeper zipkin keycloak

# Build all services
mvn clean install -DskipTests

# Start Gateway
cd gateway-service
mvn spring-boot:run

# Start IAM Service
cd iam-service
mvn spring-boot:run
```

### Access Points
- **Gateway**: http://localhost:8080
- **IAM Service**: http://localhost:8081
- **Keycloak**: http://localhost:8180
- **Zipkin**: http://localhost:9411
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

---

## 📋 Critical Files Reference

### Rate Limiting
| File | Status | Purpose |
|------|--------|---------|
| [DistributedRateLimitingFilter.java](gateway-service/src/main/java/com/enterprise/gateway/filter/DistributedRateLimitingFilter.java) | ✅ **ACTIVE** | Production rate limiting with Redis |
| ~~RateLimitingFilter.java~~ | ❌ **DELETED** | Old implementation removed (had memory leak) |

### Configuration
| File | Key Settings |
|------|-------------|
| [iam-service/application.yml](iam-service/src/main/resources/application.yml) | `maximum-pool-size: 100` (line 16) |
| [gateway-service/application.yml](gateway-service/src/main/resources/application.yml) | Resilience4j config |

### Documentation
| File | Content |
|------|---------|
| [CRITICAL_FIXES.md](CRITICAL_FIXES.md) | Detailed problem analysis and solutions |
| [VERIFICATION_CHECKLIST.md](VERIFICATION_CHECKLIST.md) | Testing and verification procedures |
| [PRODUCTION_READY_STATUS.md](PRODUCTION_READY_STATUS.md) | Production readiness report |
| [RESILIENCE_PATTERNS.md](RESILIENCE_PATTERNS.md) | Complete resilience guide (950+ lines) |

---

## 🔧 Common Commands

### Build & Deploy
```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build specific module
cd gateway-service && mvn clean install

# Build Docker images
docker-compose build

# Deploy to Kubernetes
kubectl apply -f k8s/base/
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=DistributedRateLimitingFilterTest

# Load test
ab -n 10000 -c 100 http://localhost:8080/api/users
```

### Monitoring
```bash
# Check Gateway health
curl http://localhost:8080/actuator/health

# Check IAM health
curl http://localhost:8081/actuator/health

# Check rate limit headers
curl -i http://localhost:8080/api/users | grep "X-RateLimit"

# Check connection pool
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

### Logs
```bash
# Gateway logs
docker-compose logs -f gateway-service

# IAM logs
docker-compose logs -f iam-service

# All logs
docker-compose logs -f

# Kubernetes logs
kubectl logs -f deployment/gateway-service
```

---

## 🎯 Rate Limiting

### Configuration
```yaml
# Rate limits per user tier
ratelimit:
  anonymous:
    capacity: 100      # 100 req/min
  authenticated:
    capacity: 1000     # 1000 req/min
  premium:
    capacity: 10000    # 10000 req/min
```

### Testing Rate Limits
```bash
# Test anonymous tier (100 req/min)
for i in {1..110}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/users
done | tail -10

# Expected: Last 10 requests return HTTP 429

# Test authenticated tier (1000 req/min)
TOKEN="your-jwt-token"
for i in {1..1100}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/users
done | tail -100

# Expected: Last 100 requests return HTTP 429
```

### Verify Distributed Behavior
```bash
# Scale to 5 Gateway pods
kubectl scale deployment gateway-service --replicas=5

# Send 1100 requests (should still be limited to 1000)
for i in {1..1100}; do
  curl -s -o /dev/null http://gateway:8080/api/users
done

# Check rate limit backend
curl -i http://localhost:8080/api/users | grep "X-RateLimit-Backend"
# Expected: redis (not local-cache)
```

---

## 🛡️ Resilience Patterns

### Circuit Breaker States
```bash
# Check circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Force circuit breaker to open (for testing)
# Stop IAM service
docker-compose stop iam-service

# Send requests to trigger circuit breaker
for i in {1..20}; do
  curl http://localhost:8080/api/iam/users
done

# Check state (should be OPEN)
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Restart IAM service
docker-compose start iam-service
```

### Retry Mechanism
```bash
# Watch retry logs
docker-compose logs -f gateway-service | grep "Retry"

# Expected output when service fails:
# Retry attempt 1 for service: iam-service
# Retry attempt 2 for service: iam-service
# Retry attempt 3 for service: iam-service
# Max retry attempts reached
```

### Bulkhead
```bash
# Check bulkhead metrics
curl http://localhost:8080/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls
```

---

## 🗄️ Database

### Connection Pool Monitoring
```bash
# Active connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active

# Idle connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.idle

# Pending connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.pending

# Total connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Connection timeout count
curl http://localhost:8081/actuator/metrics/hikaricp.connections.timeout
```

### Check for Connection Leaks
```bash
# Look for leak warnings in logs
docker-compose logs iam-service | grep "leak"

# Expected if leak detected:
# Connection leak detection triggered for connection, stack trace follows
```

### Optimal Pool Size Calculation
```
connections = ((core_count * 2) + effective_spindle_count)

For 8-core CPU with SSD:
connections = (8 * 2) + 1 = 17

For high concurrency with Virtual Threads:
connections = 100 (allows many Virtual Threads to wait for DB)
```

---

## 🔴 Redis

### Check Redis Connection
```bash
# Test Redis from Gateway
docker-compose exec gateway-service redis-cli -h redis ping
# Expected: PONG

# Check rate limit keys in Redis
docker-compose exec redis redis-cli KEYS "bucket:*"

# Check key expiration
docker-compose exec redis redis-cli TTL "bucket:ip:127.0.0.1"
```

### Test Redis Failover
```bash
# Stop Redis
docker-compose stop redis

# Test API still works (should fallback to local cache)
curl -i http://localhost:8080/api/users | grep "X-RateLimit-Backend"
# Expected: local-cache

# Restart Redis
docker-compose start redis

# Wait 10 seconds and test again
sleep 10
curl -i http://localhost:8080/api/users | grep "X-RateLimit-Backend"
# Expected: redis (auto-recovered)
```

---

## 📊 Metrics & Monitoring

### Prometheus Metrics
```bash
# Scrape endpoint
curl http://localhost:8080/actuator/prometheus

# Key metrics to monitor:
# - hikaricp_connections_active
# - hikaricp_connections_max
# - resilience4j_circuitbreaker_state
# - resilience4j_ratelimiter_available_permissions
# - http_server_requests_seconds
# - jvm_memory_used_bytes
```

### Grafana Setup
```bash
# Access Grafana
open http://localhost:3000

# Default credentials
# Username: admin
# Password: admin

# Add Prometheus datasource
# URL: http://prometheus:9090

# Import dashboards:
# - Circuit Breaker: See RESILIENCE_PATTERNS.md
# - Rate Limiting: See RESILIENCE_PATTERNS.md
# - Connection Pool: See RESILIENCE_PATTERNS.md
```

### Zipkin Tracing
```bash
# Access Zipkin
open http://localhost:9411

# Search for traces
# - Service: gateway-service
# - Span: filter execution
# - Look for: resilience4j decorators
```

---

## 🧪 Testing Scenarios

### Scenario 1: Memory Leak Prevention
```bash
# Run for 1 hour, monitor memory
while true; do
  curl -s http://localhost:8080/api/users > /dev/null
  sleep 0.1
done &

# Monitor memory every 5 minutes
while true; do
  docker stats gateway-service --no-stream --format "{{.MemUsage}}"
  sleep 300
done

# Expected: Memory stays constant (~200MB, NOT growing to 500MB+)
```

### Scenario 2: Distributed Rate Limiting
```bash
# Terminal 1: Start Gateway Pod 1
docker-compose up gateway-service

# Terminal 2: Start Gateway Pod 2 (on different port)
PORT=8081 docker-compose up gateway-service

# Terminal 3: Send requests to both pods
for i in {1..500}; do curl -s http://localhost:8080/api/users > /dev/null; done &
for i in {1..500}; do curl -s http://localhost:8081/api/users > /dev/null; done &

# Total: 1000 requests across 2 pods
# Expected: Some requests get HTTP 429 (total limit is 1000, NOT 2000)
```

### Scenario 3: Connection Pool Under Load
```bash
# Load test with 500 concurrent requests
ab -n 10000 -c 500 http://localhost:8080/api/iam/users

# Monitor connection pool during test
watch -n 1 'curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq'

# Expected:
# - Active connections: 60-80 (NOT maxing out at 100)
# - No connection timeout errors
# - Requests complete successfully
```

### Scenario 4: Circuit Breaker Behavior
```bash
# Stop IAM service
docker-compose stop iam-service

# Send 20 requests to open circuit breaker
for i in {1..20}; do
  curl -s http://localhost:8080/api/iam/users
done

# Check circuit breaker state
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
# Expected: state = "OPEN"

# Restart IAM service
docker-compose start iam-service

# Wait for circuit breaker to transition to HALF_OPEN (10 seconds)
sleep 10

# Send test request
curl http://localhost:8080/api/iam/users

# Circuit breaker should close if successful
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
# Expected: state = "CLOSED"
```

---

## 🚨 Troubleshooting

### Issue: Gateway Returns HTTP 500
```bash
# Check Gateway logs
docker-compose logs gateway-service | tail -50

# Common causes:
# 1. Redis connection failed → Check Redis is running
# 2. Downstream service down → Check circuit breaker state
# 3. Database connection timeout → Check connection pool
```

### Issue: Rate Limiting Not Working
```bash
# Verify only new filter exists
find gateway-service -name "*RateLimitingFilter.java"
# Should see ONLY: DistributedRateLimitingFilter.java

# Check filter initialization in logs
docker-compose logs gateway-service | grep "Distributed Rate Limiting"
# Expected: ✅ Distributed Rate Limiting initialized with Redis backend

# Verify Redis connection
docker-compose logs gateway-service | grep -i redis
# Should show successful Redis connection
```

### Issue: Connection Pool Exhausted
```bash
# Check pool metrics
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
curl http://localhost:8081/actuator/metrics/hikaricp.connections.pending

# If pending > 0:
# 1. Check for connection leaks in logs
# 2. Increase pool size if needed (max 100)
# 3. Check database is responsive

# If active = max (100):
# Check if load is actually high or if there's a slow query
docker-compose exec postgres psql -U postgres -d iam_db -c "SELECT * FROM pg_stat_activity;"
```

### Issue: Memory Usage Growing
```bash
# Check memory usage
docker stats gateway-service

# If growing over time:
# 1. Check Caffeine cache is configured (max 50K entries)
# 2. Check cache eviction is working
# 3. Look for heap dump

# Take heap dump
docker exec gateway-service jmap -dump:live,format=b,file=/tmp/heap.bin <PID>

# Analyze with Eclipse MAT or VisualVM
```

### Issue: Redis Connection Failed
```bash
# Check Redis is running
docker-compose ps redis

# Test Redis connection
docker-compose exec redis redis-cli ping
# Expected: PONG

# Check Gateway can reach Redis
docker-compose exec gateway-service ping redis
# Expected: successful ping

# Check Gateway logs for Redis errors
docker-compose logs gateway-service | grep -i redis

# System should fallback to local cache gracefully
curl -i http://localhost:8080/api/users | grep "X-RateLimit-Backend"
# Expected: local-cache (if Redis down)
```

---

## 🔐 Security

### OAuth2 Testing
```bash
# Get access token from Keycloak
curl -X POST "http://localhost:8180/realms/enterprise/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=gateway-client" \
  -d "username=testuser" \
  -d "password=testpass" \
  -d "scope=openid profile email" \
  | jq -r '.access_token'

# Use token in request
TOKEN="<access_token>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/iam/users
```

### JWT Verification
```bash
# Decode JWT (header.payload.signature)
echo "<token>" | cut -d. -f2 | base64 -d | jq

# Expected claims:
# - sub: user ID
# - realm_access.roles: ["user", "admin"]
# - exp: expiration timestamp
```

---

## 📈 Performance Tuning

### JVM Settings
```bash
# Gateway (Reactive - less heap needed)
java -Xms512m -Xmx1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar gateway-service.jar

# IAM Service (Virtual Threads - more heap for threads)
java -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -jar iam-service.jar
```

### Virtual Threads Configuration
```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true

# Tomcat with Virtual Threads
server:
  tomcat:
    threads:
      max: 1000          # High with Virtual Threads
      min-spare: 100
```

### Caffeine Cache Tuning
```java
// Increase cache size if needed
private final Cache<String, Bucket> localCache = Caffeine.newBuilder()
    .maximumSize(100_000)              // Increase from 50K if needed
    .expireAfterWrite(Duration.ofMinutes(10))  // Increase TTL if needed
    .recordStats()
    .build();
```

---

## 📚 Additional Resources

### Documentation
- [README.md](README.md) - Project overview
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture
- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide
- [RESILIENCE_PATTERNS.md](RESILIENCE_PATTERNS.md) - Complete resilience guide
- [CRITICAL_FIXES.md](CRITICAL_FIXES.md) - Critical issues and fixes

### External Links
- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Resilience4j Docs](https://resilience4j.readme.io/)
- [Bucket4j Docs](https://github.com/bucket4j/bucket4j)
- [Caffeine Docs](https://github.com/ben-manes/caffeine)
- [HikariCP Docs](https://github.com/brettwooldridge/HikariCP)
- [Virtual Threads JEP](https://openjdk.org/jeps/444)

---

## 🎓 Best Practices

### DO ✅
- ✅ Use DistributedRateLimitingFilter (Redis-backed)
- ✅ Monitor connection pool usage
- ✅ Set cache eviction policies (max size + TTL)
- ✅ Enable graceful degradation
- ✅ Use circuit breakers for external calls
- ✅ Add retry with exponential backoff
- ✅ Monitor metrics (Prometheus + Grafana)
- ✅ Use distributed tracing (Zipkin)

### DON'T ❌
- ❌ Use unbounded collections without eviction (like the old RateLimitingFilter that was deleted)
- ❌ Use unbounded caches (ConcurrentHashMap without eviction)
- ❌ Forget to configure connection pool leak detection
- ❌ Disable circuit breakers in production
- ❌ Skip monitoring and alerting
- ❌ Use blocking code in Gateway (WebFlux)
- ❌ Set retry without exponential backoff
- ❌ Run production without load testing

---

## 📞 Support

### Getting Help
1. Check logs: `docker-compose logs -f <service>`
2. Check health: `curl http://localhost:808x/actuator/health`
3. Check metrics: `curl http://localhost:808x/actuator/metrics`
4. Review documentation in this repository
5. Check Grafana dashboards for system health

### Common Issues
- **HTTP 500**: Check downstream service health and circuit breaker state
- **HTTP 429**: Rate limit exceeded - normal behavior
- **Connection timeout**: Check connection pool usage
- **Memory leak**: Verify Caffeine cache eviction is working

---

**Document Version**: 1.0
**Last Updated**: December 30, 2024
