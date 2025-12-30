# Concurrency & Monitoring Setup Walkthrough

I have configured a monitoring solution and a load testing script to help you determine and monitor the Number of Concurrent Users.

## 1. Monitor Dashboard (Concurrent Users)

I have successfully configured **Grafana** to automatically provision a dashboard for monitoring concurrency.

### How to access:
1. Restart your infrastructure to apply the new Grafana configs:
   ```bash
   docker-compose down
   docker-compose up -d
   ```
2. Open Grafana: [http://localhost:3000](http://localhost:3000) (Login: `admin`/`admin`)
3. Go to **Dashboards**, you will see a new dashboard titled **"Microservices Sync Dashboard"**.

### Metrics included:
- **Active User Sessions (Logged In)**: [NEW] Shows the real-time number of users currently logged in via Keycloak.
- **Real-time Request Activity**: Shows the number of requests currently being processed by the backend.
- **Requests Per Second (Throughput)**: The rate of traffic.
- **99th Percentile Latency**: To ensure performance doesn't degrade under load.

## 2. Real-time User Monitoring vs. Test Data
You asked how to see **real users** vs test data.

- **Active User Sessions**: This metric comes directly from **Keycloak (Identity Provider)**. It counts exactly how many users have a valid session token. This is the **Active Users** count.
- **Request Activity**: This shows users currently *doing something* (clicking, loading).

To simulate this for testing:
1.  Log in manually to the application (incognito windows).
2.  Or run the load test script.

## 3. Determining "How many concurrent users?"

The number of concurrent users your system can handle depends on the weakest link, usually:
1.  **Database Connection Pool**: Currently defaults to ~10 connections per service (HikariCP default). This is likely your hard limit (~10-20 concurrent requests involving DB).
2.  **Memory/CPU**: Docker limits.

### Solution: Load Testing
I have created a **K6 Load Test Script** to verify the actual capacity.

**File Location**: `infrastructure/load-tests/k6-load-test.js`

**How to run (using Docker):**
```bash
docker run --rm -i grafana/k6 run - < infrastructure/load-tests/k6-load-test.js
```

This script will:
- Simulate 50 -> 100 concurrent users.
- Hit the Gateway (`localhost:8080`).
- You can watch the **Grafana Dashboard** while this test runs to see the "Concurrent Active Requests" spike and check if Latency increases.

## Recommendation for Scaling
To increase concurrent users:
1.  **Increase DB Pool Size**: In `application.yml`, set `spring.datasource.hikari.maximum-pool-size: 50` (or higher).
2.  **Scale Services**: Use `docker-compose up -d --scale business-service=3`.
