# Performance Test Guide

## Overview

This directory contains performance and load testing scripts using [k6](https://k6.io/) - a modern load testing tool.

## Prerequisites

```bash
# Install k6
# Windows (using Chocolatey)
choco install k6

# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## Test Scripts

### 1. load-test.js

**Purpose:** Simulate realistic user load to validate system can handle target concurrent users.

**Configuration:**
- Target: 1000 concurrent users
- Duration: ~23 minutes
- Ramp-up stages:
  - 10 users (warm-up)
  - 100 users → 500 users → 1000 users
  - Gradual ramp-down

**Performance Thresholds:**
- p95 latency < 500ms
- p99 latency < 1000ms
- Error rate < 5%
- Product creation < 800ms
- Approval < 600ms

**Run:**
```bash
# Local testing
k6 run load-test.js

# Against staging
K6_TARGET_URL=https://staging.example.com k6 run load-test.js

# With authentication
K6_TARGET_URL=https://api.example.com AUTH_TOKEN=your_token k6 run load-test.js

# Output to file
k6 run load-test.js --out json=load-test-results.json
```

### 2. stress-test.js

**Purpose:** Push system beyond normal capacity to find breaking points.

**Configuration:**
- Target: 2000 concurrent users (2x capacity)
- Duration: ~16 minutes
- Aggressive ramp-up to stress system
- Recovery test included

**Thresholds** (more lenient):
- p95 latency < 2000ms
- p99 latency < 5000ms
- Error rate < 15%

**Run:**
```bash
k6 run stress-test.js

# With custom target
K6_TARGET_URL=https://staging.example.com k6 run stress-test.js
```

## Test Scenarios

### Scenario 1: Product Creation Flow

```javascript
// Creates product → triggers workflow → checks status
POST /api/products
GET /api/products (list with pagination)
GET /api/products/{id}
GET /api/products?status=PENDING_APPROVAL
```

**Validates:**
- API response time
- Database query performance
- Cache effectiveness
- Workflow trigger latency

### Scenario 2: Cache Performance

```javascript
// First request (cache miss)
GET /api/products/{id}  // ~100-200ms (database query)

// Subsequent requests (cache hit)
GET /api/products/{id}  // ~5-10ms (from cache)
```

**Validates:**
- Cache hit rate > 80%
- Cache TTL effectiveness
- Redis performance

### Scenario 3: Search & Filter

```javascript
GET /api/products?status=PENDING_APPROVAL&page=0&size=10
GET /api/products?category=ELECTRONICS
```

**Validates:**
- Database index usage
- Query optimization
- Pagination performance

## Interpreting Results

### Good Performance Indicators

```
✓ http_req_duration..............: avg=250ms min=10ms med=200ms max=800ms p(95)=450ms p(99)=700ms
✓ http_req_failed................: 1.5% (< 5% threshold)
✓ product_creation_time..........: avg=400ms p(95)=650ms (< 800ms threshold)
✓ iterations.....................: 15000/23min (650/sec)
```

**Interpretation:**
- ✅ All thresholds met
- ✅ 95% of requests < 500ms
- ✅ Error rate acceptable
- ✅ High throughput (650 req/sec)

### Performance Issues

```
✗ http_req_duration..............: avg=1200ms p(95)=2500ms p(99)=5000ms
✗ http_req_failed................: 8.5% (> 5% threshold)
✗ product_creation_time..........: avg=1800ms p(95)=3200ms
```

**Interpretation:**
- ❌ Latency too high (investigate bottlenecks)
- ❌ Error rate above threshold (check logs)
- ❌ System overloaded or misconfigured

## Metrics Explained

| Metric | Description | Good Value |
|--------|-------------|------------|
| `http_req_duration` | Total request duration | p95 < 500ms |
| `http_req_waiting` | Time waiting for response | p95 < 400ms |
| `http_req_connecting` | TCP connection time | avg < 5ms |
| `http_req_blocked` | Time blocked before request | avg < 1ms |
| `http_req_failed` | Failed requests % | < 5% |
| `iterations` | Total completed iterations | Higher is better |
| `vus` | Virtual users active | Matches target |

## Troubleshooting

### High Latency (p95 > 500ms)

**Check:**
1. Database slow queries
   ```sql
   SELECT query, mean_exec_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_exec_time DESC LIMIT 10;
   ```

2. Cache hit rate
   ```bash
   redis-cli info stats | grep keyspace_hits
   redis-cli info stats | grep keyspace_misses
   ```

3. Application logs for bottlenecks

**Solutions:**
- Add database indexes
- Increase cache TTL
- Optimize slow queries
- Scale horizontally (add pods)

### High Error Rate (> 5%)

**Check:**
1. Application logs
   ```bash
   kubectl logs -f deployment/business-service -n product-mgmt-prod | grep ERROR
   ```

2. Database connections
   ```sql
   SELECT count(*) FROM pg_stat_activity;
   ```

3. RabbitMQ queue depth
   ```bash
   curl -u guest:guest http://localhost:15672/api/queues
   ```

**Solutions:**
- Increase connection pool size
- Add rate limiting
- Check for database deadlocks
- Verify RabbitMQ consumer count

### Memory Issues

**Check:**
```bash
# Kubernetes pods
kubectl top pods -n product-mgmt-prod

# JVM heap usage
kubectl exec -it deployment/business-service -n product-mgmt-prod -- \
  jcmd 1 GC.heap_info
```

**Solutions:**
- Increase memory limits in K8s
- Tune JVM heap size (-XX:MaxRAMPercentage)
- Check for memory leaks
- Review cache size limits

## Performance Optimization Checklist

- [ ] Database indexes created
- [ ] Cache configuration tuned
- [ ] Connection pools optimized
- [ ] RabbitMQ prefetch configured
- [ ] HPA thresholds set
- [ ] Load test passing
- [ ] Stress test completed
- [ ] Metrics dashboards reviewed

## CI/CD Integration

Add to GitHub Actions:

```yaml
- name: Run Performance Tests
  run: |
    k6 run performance-tests/load-test.js \
      --out json=performance-results.json
  env:
    K6_TARGET_URL: https://staging.example.com
    AUTH_TOKEN: ${{ secrets.TEST_AUTH_TOKEN }}

- name: Upload Results
  uses: actions/upload-artifact@v3
  with:
    name: performance-results
    path: performance-results.json
```

## Performance Targets

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Concurrent Users | 1000 | TBD | 🎯 |
| p95 Latency | < 500ms | TBD | 🎯 |
| p99 Latency | < 1000ms | TBD | 🎯 |
| Error Rate | < 5% | TBD | 🎯 |
| Throughput | > 500 req/s | TBD | 🎯 |
| Cache Hit Rate | > 80% | TBD | 🎯 |

## Next Steps

1. **Baseline Test:**
   ```bash
   k6 run --vus 10 --duration 30s load-test.js
   ```

2. **Run Full Load Test:**
   ```bash
   k6 run load-test.js --out json=results/baseline-$(date +%Y%m%d).json
   ```

3. **Analyze Results:**
   - Review Grafana dashboards
   - Check Prometheus metrics
   - Identify bottlenecks

4. **Optimize:**
   - Apply performance improvements
   - Re-run tests
   - Compare results

5. **Document:**
   - Update performance targets
   - Record baseline metrics
   - Note optimization impact
