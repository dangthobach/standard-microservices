# 🔌 Backend Integration Guide - Dashboard Metrics

## 📋 Overview

Hướng dẫn tích hợp Dashboard với Backend API thật. Hiện tại Dashboard đang dùng mock data từ `MetricsService`. Document này hướng dẫn cách thay thế mock data bằng real API calls.

---

## 🎯 API Endpoints Cần Thiết

### 1. Real-time Metrics API
**Endpoint**: `GET /api/metrics/realtime`

**Response Format**:
```json
{
  "ccu": 1247,
  "rps": 3542,
  "errorRate": 0.12,
  "avgLatency": 145
}
```

**TypeScript Interface**: `RealtimeMetrics`

---

### 2. Service Health API
**Endpoint**: `GET /api/metrics/services`

**Response Format**:
```json
[
  {
    "name": "Gateway Service",
    "status": "healthy",
    "cpu": 45.2,
    "memory": 62.8,
    "uptime": "15d 7h 23m",
    "requests": 125432,
    "errors": 12
  },
  {
    "name": "IAM Service",
    "status": "healthy",
    "cpu": 32.1,
    "memory": 48.3,
    "uptime": "15d 7h 23m",
    "requests": 45231,
    "errors": 3
  }
]
```

**TypeScript Interface**: `ServiceHealth[]`

**Status Values**: `"healthy" | "warning" | "critical" | "down"`

---

### 3. Traffic History API
**Endpoint**: `GET /api/metrics/traffic?hours=24`

**Query Parameters**:
- `hours`: Number of hours to fetch (default: 24)

**Response Format**:
```json
[
  {
    "timestamp": "2026-01-01T00:00:00Z",
    "requests": 3234,
    "errors": 12
  },
  {
    "timestamp": "2026-01-01T01:00:00Z",
    "requests": 3567,
    "errors": 8
  }
]
```

**TypeScript Interface**: `TrafficData[]`

---

### 4. Latency Metrics API
**Endpoint**: `GET /api/metrics/latency`

**Response Format**:
```json
[
  {
    "service": "Gateway",
    "p50": 45,
    "p95": 145,
    "p99": 285
  },
  {
    "service": "IAM",
    "p50": 32,
    "p95": 98,
    "p99": 178
  }
]
```

**TypeScript Interface**: `LatencyData[]`

---

### 5. Database Metrics API
**Endpoint**: `GET /api/metrics/database`

**Response Format**:
```json
[
  {
    "name": "Primary DB",
    "connections": 87,
    "maxConnections": 100,
    "activeQueries": 12,
    "slowQueries": 3,
    "cacheHitRate": 94.5
  },
  {
    "name": "Replica DB",
    "connections": 45,
    "maxConnections": 100,
    "activeQueries": 8,
    "slowQueries": 1,
    "cacheHitRate": 96.2
  }
]
```

**TypeScript Interface**: `DatabaseMetrics[]`

---

### 6. Redis Metrics API
**Endpoint**: `GET /api/metrics/redis`

**Response Format**:
```json
{
  "connections": 234,
  "memoryUsed": 1.8,
  "memoryTotal": 4.0,
  "hitRate": 98.7,
  "evictions": 123,
  "opsPerSec": 15234
}
```

**TypeScript Interface**: `RedisMetrics`

---

### 7. Slow Endpoints API
**Endpoint**: `GET /api/metrics/slow-endpoints?limit=5`

**Query Parameters**:
- `limit`: Number of endpoints to return (default: 5)

**Response Format**:
```json
[
  {
    "method": "POST",
    "path": "/api/analytics/report",
    "avgLatency": 1245,
    "p95Latency": 2345,
    "calls": 1234
  },
  {
    "method": "GET",
    "path": "/api/business/dashboard",
    "avgLatency": 876,
    "p95Latency": 1567,
    "calls": 5678
  }
]
```

**TypeScript Interface**: `SlowEndpoint[]`

---

## 🔧 Implementation Steps

### Step 1: Create API Service

Tạo file mới: `src/app/shared/services/metrics-api.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  RealtimeMetrics,
  ServiceHealth,
  TrafficData,
  LatencyData,
  DatabaseMetrics,
  RedisMetrics,
  SlowEndpoint
} from '../models/metrics.model';

@Injectable({
  providedIn: 'root'
})
export class MetricsApiService {
  private apiUrl = `${environment.apiUrl}/metrics`;

  constructor(private http: HttpClient) {}

  getRealtimeMetrics(): Observable<RealtimeMetrics> {
    return this.http.get<RealtimeMetrics>(`${this.apiUrl}/realtime`);
  }

  getServices(): Observable<ServiceHealth[]> {
    return this.http.get<ServiceHealth[]>(`${this.apiUrl}/services`);
  }

  getTrafficHistory(hours: number = 24): Observable<TrafficData[]> {
    return this.http.get<TrafficData[]>(`${this.apiUrl}/traffic?hours=${hours}`);
  }

  getLatencyMetrics(): Observable<LatencyData[]> {
    return this.http.get<LatencyData[]>(`${this.apiUrl}/latency`);
  }

  getDatabaseMetrics(): Observable<DatabaseMetrics[]> {
    return this.http.get<DatabaseMetrics[]>(`${this.apiUrl}/database`);
  }

  getRedisMetrics(): Observable<RedisMetrics> {
    return this.http.get<RedisMetrics>(`${this.apiUrl}/redis`);
  }

  getSlowEndpoints(limit: number = 5): Observable<SlowEndpoint[]> {
    return this.http.get<SlowEndpoint[]>(`${this.apiUrl}/slow-endpoints?limit=${limit}`);
  }
}
```

---

### Step 2: Update MetricsService to Use Real API

Sửa file: `src/app/shared/services/metrics.service.ts`

```typescript
import { Injectable, signal } from '@angular/core';
import { MetricsApiService } from './metrics-api.service';
import {
  RealtimeMetrics,
  ServiceHealth,
  TrafficData,
  LatencyData,
  DatabaseMetrics,
  RedisMetrics,
  SlowEndpoint,
  TimeSeriesData
} from '../models/metrics.model';

@Injectable({
  providedIn: 'root'
})
export class MetricsService {
  private realtimeMetrics = signal<RealtimeMetrics>({
    ccu: 0,
    rps: 0,
    errorRate: 0,
    avgLatency: 0
  });

  private services = signal<ServiceHealth[]>([]);
  private trafficHistory = signal<TrafficData[]>([]);
  private latencyMetrics = signal<LatencyData[]>([]);
  private databaseMetrics = signal<DatabaseMetrics[]>([]);
  private redisMetrics = signal<RedisMetrics | null>(null);
  private slowEndpoints = signal<SlowEndpoint[]>([]);

  constructor(private apiService: MetricsApiService) {
    this.startRealtimeUpdates();
  }

  getRealtimeMetrics() {
    return this.realtimeMetrics.asReadonly();
  }

  getServices() {
    return this.services.asReadonly();
  }

  getTrafficHistory() {
    return this.trafficHistory.asReadonly();
  }

  getLatencyMetrics() {
    return this.latencyMetrics.asReadonly();
  }

  getDatabaseMetrics() {
    return this.databaseMetrics.asReadonly();
  }

  getRedisMetrics() {
    return this.redisMetrics.asReadonly();
  }

  getSlowEndpoints() {
    return this.slowEndpoints.asReadonly();
  }

  getTrafficChartData(): TimeSeriesData[] {
    const history = this.trafficHistory();
    return [
      {
        name: 'Requests',
        series: history.map(d => ({
          name: new Date(d.timestamp).toLocaleTimeString(),
          value: d.requests
        }))
      },
      {
        name: 'Errors',
        series: history.map(d => ({
          name: new Date(d.timestamp).toLocaleTimeString(),
          value: d.errors
        }))
      }
    ];
  }

  private startRealtimeUpdates(): void {
    // Initial load
    this.loadAllMetrics();

    // Auto-refresh every 3 seconds
    setInterval(() => {
      this.loadAllMetrics();
    }, 3000);
  }

  private loadAllMetrics(): void {
    // Realtime metrics
    this.apiService.getRealtimeMetrics().subscribe({
      next: (data) => this.realtimeMetrics.set(data),
      error: (err) => console.error('Error loading realtime metrics:', err)
    });

    // Services
    this.apiService.getServices().subscribe({
      next: (data) => this.services.set(data),
      error: (err) => console.error('Error loading services:', err)
    });

    // Traffic history
    this.apiService.getTrafficHistory().subscribe({
      next: (data) => this.trafficHistory.set(data),
      error: (err) => console.error('Error loading traffic history:', err)
    });

    // Latency metrics
    this.apiService.getLatencyMetrics().subscribe({
      next: (data) => this.latencyMetrics.set(data),
      error: (err) => console.error('Error loading latency metrics:', err)
    });

    // Database metrics
    this.apiService.getDatabaseMetrics().subscribe({
      next: (data) => this.databaseMetrics.set(data),
      error: (err) => console.error('Error loading database metrics:', err)
    });

    // Redis metrics
    this.apiService.getRedisMetrics().subscribe({
      next: (data) => this.redisMetrics.set(data),
      error: (err) => console.error('Error loading redis metrics:', err)
    });

    // Slow endpoints
    this.apiService.getSlowEndpoints().subscribe({
      next: (data) => this.slowEndpoints.set(data),
      error: (err) => console.error('Error loading slow endpoints:', err)
    });
  }
}
```

---

### Step 3: Update Environment Configuration

Sửa file: `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api', // Your backend URL
  keycloakUrl: 'http://localhost:8081',
  keycloakRealm: 'microservices',
  keycloakClientId: 'gateway-client'
};
```

---

## 🔄 Backend Implementation Example (Spring Boot)

### MetricsController.java

```java
package com.enterprise.gateway.controller;

import com.enterprise.gateway.dto.*;
import com.enterprise.gateway.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/realtime")
    public RealtimeMetricsDTO getRealtimeMetrics() {
        return metricsService.getRealtimeMetrics();
    }

    @GetMapping("/services")
    public List<ServiceHealthDTO> getServices() {
        return metricsService.getServiceHealth();
    }

    @GetMapping("/traffic")
    public List<TrafficDataDTO> getTrafficHistory(@RequestParam(defaultValue = "24") int hours) {
        return metricsService.getTrafficHistory(hours);
    }

    @GetMapping("/latency")
    public List<LatencyDataDTO> getLatencyMetrics() {
        return metricsService.getLatencyMetrics();
    }

    @GetMapping("/database")
    public List<DatabaseMetricsDTO> getDatabaseMetrics() {
        return metricsService.getDatabaseMetrics();
    }

    @GetMapping("/redis")
    public RedisMetricsDTO getRedisMetrics() {
        return metricsService.getRedisMetrics();
    }

    @GetMapping("/slow-endpoints")
    public List<SlowEndpointDTO> getSlowEndpoints(@RequestParam(defaultValue = "5") int limit) {
        return metricsService.getSlowEndpoints(limit);
    }
}
```

---

## 📊 Data Collection Strategy (Backend)

### 1. Real-time Metrics
**Source**: Spring Boot Actuator + Custom Metrics

```java
@Service
public class MetricsCollectorService {

    @Autowired
    private MeterRegistry meterRegistry;

    public RealtimeMetricsDTO collectRealtimeMetrics() {
        Counter requestCounter = meterRegistry.counter("http.server.requests");
        Timer requestTimer = meterRegistry.timer("http.server.requests");

        return RealtimeMetricsDTO.builder()
            .ccu(getCurrentConcurrentUsers())
            .rps(calculateRequestsPerSecond(requestCounter))
            .errorRate(calculateErrorRate())
            .avgLatency(requestTimer.mean(TimeUnit.MILLISECONDS))
            .build();
    }
}
```

### 2. Service Health
**Source**: Eureka / Consul + Actuator Health Endpoints

```java
@Service
public class ServiceHealthService {

    @Autowired
    private DiscoveryClient discoveryClient;

    public List<ServiceHealthDTO> getServiceHealth() {
        return discoveryClient.getServices().stream()
            .map(this::collectServiceMetrics)
            .collect(Collectors.toList());
    }

    private ServiceHealthDTO collectServiceMetrics(String serviceName) {
        // Query service actuator metrics endpoint
        // /actuator/metrics/process.cpu.usage
        // /actuator/metrics/jvm.memory.used
        // Calculate from Micrometer
    }
}
```

### 3. Traffic History
**Source**: Time-series Database (InfluxDB / Prometheus)

```java
@Service
public class TrafficHistoryService {

    @Autowired
    private InfluxDBTemplate influxDBTemplate;

    public List<TrafficDataDTO> getTrafficHistory(int hours) {
        String query = String.format(
            "SELECT SUM(requests), SUM(errors) FROM traffic " +
            "WHERE time > now() - %dh GROUP BY time(1h)",
            hours
        );
        return influxDBTemplate.query(query, TrafficDataDTO.class);
    }
}
```

### 4. Database Metrics
**Source**: JDBC Metadata + Connection Pool

```java
@Service
public class DatabaseMetricsService {

    @Autowired
    private DataSource dataSource;

    public List<DatabaseMetricsDTO> getDatabaseMetrics() {
        HikariDataSource hikari = (HikariDataSource) dataSource;
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();

        return List.of(
            DatabaseMetricsDTO.builder()
                .name("Primary DB")
                .connections(pool.getActiveConnections())
                .maxConnections(hikari.getMaximumPoolSize())
                .activeQueries(getActiveQueryCount())
                .slowQueries(getSlowQueryCount())
                .cacheHitRate(calculateCacheHitRate())
                .build()
        );
    }
}
```

### 5. Redis Metrics
**Source**: Redis INFO command

```java
@Service
public class RedisMetricsService {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    public RedisMetricsDTO getRedisMetrics() {
        RedisConnection connection = connectionFactory.getConnection();
        Properties info = connection.info();

        return RedisMetricsDTO.builder()
            .connections(Integer.parseInt(info.getProperty("connected_clients")))
            .memoryUsed(parseMemory(info.getProperty("used_memory")))
            .memoryTotal(parseMemory(info.getProperty("maxmemory")))
            .hitRate(calculateHitRate(info))
            .evictions(Long.parseLong(info.getProperty("evicted_keys")))
            .opsPerSec(Long.parseLong(info.getProperty("instantaneous_ops_per_sec")))
            .build();
    }
}
```

---

## ⚠️ Important Notes

### Date Handling
Backend cần trả về timestamp theo format ISO 8601:
```json
"timestamp": "2026-01-01T12:34:56.789Z"
```

Frontend sẽ parse bằng:
```typescript
new Date(data.timestamp)
```

### Error Handling
Nên có fallback data khi API fail:
```typescript
this.apiService.getRealtimeMetrics().subscribe({
  next: (data) => this.realtimeMetrics.set(data),
  error: (err) => {
    console.error('Error loading realtime metrics:', err);
    // Keep showing last known data instead of crashing
  }
});
```

### CORS Configuration
Backend phải enable CORS cho frontend origin:
```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:4200")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowCredentials(true);
            }
        };
    }
}
```

---

## 🧪 Testing API Integration

### Step 1: Verify API Endpoints
```bash
# Test realtime metrics
curl http://localhost:8080/api/metrics/realtime

# Test services
curl http://localhost:8080/api/metrics/services

# Test traffic
curl http://localhost:8080/api/metrics/traffic?hours=24
```

### Step 2: Check Network Tab
- Open browser DevTools → Network
- Navigate to Dashboard
- Verify API calls every 3 seconds
- Check response format matches TypeScript interfaces

### Step 3: Monitor Console
- No errors in browser console
- No 404 or 500 errors
- Data updates smoothly

---

## 🔀 Gradual Migration Plan

### Phase 1: Keep Mock Data (Current)
✅ Dashboard works with mock data
✅ UI fully functional
✅ No backend dependency

### Phase 2: Add API Service
- Create `MetricsApiService`
- Keep mock data as fallback
- Test API calls separately

### Phase 3: Switch to Real API
- Update `MetricsService` to use `MetricsApiService`
- Add error handling
- Keep mock data as fallback on error

### Phase 4: Remove Mock Data
- Delete mock data generation code
- Pure API-driven dashboard
- Add loading states

---

## 📈 Performance Optimization

### Caching Strategy
```typescript
// Cache metrics for 3 seconds to avoid redundant calls
private cache = new Map<string, { data: any, timestamp: number }>();

private getCached<T>(key: string, apiCall: Observable<T>): Observable<T> {
  const cached = this.cache.get(key);
  const now = Date.now();

  if (cached && (now - cached.timestamp) < 3000) {
    return of(cached.data as T);
  }

  return apiCall.pipe(
    tap(data => this.cache.set(key, { data, timestamp: now }))
  );
}
```

### WebSocket Alternative
Nếu cần real-time hơn, dùng WebSocket thay vì polling:
```typescript
// ws://localhost:8080/ws/metrics
this.wsService.connect('/ws/metrics').subscribe(data => {
  this.realtimeMetrics.set(data);
});
```

---

## ✅ Checklist Before Going Live

- [ ] All API endpoints implemented
- [ ] CORS configured correctly
- [ ] Date format consistent (ISO 8601)
- [ ] Error handling added
- [ ] Loading states implemented
- [ ] Fallback data on error
- [ ] Performance tested (no memory leaks)
- [ ] Network throttling tested
- [ ] API rate limiting considered
- [ ] Security (authentication) added

---

## 📝 Summary

### Current State (Mock Data)
```
Frontend (MetricsService) → Mock Data Generator → Dashboard
```

### Target State (Real API)
```
Frontend (MetricsService) → MetricsApiService → Backend API → Dashboard
```

### Files to Create/Modify
1. ✅ Create: `metrics-api.service.ts`
2. ✅ Modify: `metrics.service.ts`
3. ✅ Modify: `environment.ts`
4. Backend: Create controllers & services

---

**Ready for Backend Integration! 🚀**
