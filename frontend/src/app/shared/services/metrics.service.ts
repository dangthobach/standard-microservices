import { Injectable, signal } from '@angular/core';
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

  constructor() {
    this.initializeMockData();
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
          name: d.timestamp.toLocaleTimeString(),
          value: d.requests
        }))
      },
      {
        name: 'Errors',
        series: history.map(d => ({
          name: d.timestamp.toLocaleTimeString(),
          value: d.errors
        }))
      }
    ];
  }

  private initializeMockData(): void {
    // Initialize realtime metrics
    this.realtimeMetrics.set({
      ccu: 1247,
      rps: 3542,
      errorRate: 0.12,
      avgLatency: 145
    });

    // Initialize services
    this.services.set([
      {
        name: 'Gateway Service',
        status: 'healthy',
        cpu: 45.2,
        memory: 62.8,
        uptime: '15d 7h 23m',
        requests: 125432,
        errors: 12
      },
      {
        name: 'IAM Service',
        status: 'healthy',
        cpu: 32.1,
        memory: 48.3,
        uptime: '15d 7h 23m',
        requests: 45231,
        errors: 3
      },
      {
        name: 'Business Service',
        status: 'warning',
        cpu: 78.5,
        memory: 81.2,
        uptime: '7d 12h 45m',
        requests: 89765,
        errors: 152
      },
      {
        name: 'Notification Service',
        status: 'healthy',
        cpu: 28.3,
        memory: 35.7,
        uptime: '22d 3h 12m',
        requests: 34521,
        errors: 5
      },
      {
        name: 'Payment Service',
        status: 'healthy',
        cpu: 52.7,
        memory: 67.4,
        uptime: '10d 18h 34m',
        requests: 23456,
        errors: 8
      },
      {
        name: 'Analytics Service',
        status: 'critical',
        cpu: 92.1,
        memory: 94.5,
        uptime: '2d 5h 23m',
        requests: 156789,
        errors: 1234
      }
    ]);

    // Initialize traffic history (last 24 hours)
    const history: TrafficData[] = [];
    const now = new Date();
    for (let i = 23; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - i * 3600000);
      history.push({
        timestamp,
        requests: Math.floor(3000 + Math.random() * 2000 + Math.sin(i / 4) * 1000),
        errors: Math.floor(5 + Math.random() * 20)
      });
    }
    this.trafficHistory.set(history);

    // Initialize latency metrics
    this.latencyMetrics.set([
      { service: 'Gateway', p50: 45, p95: 145, p99: 285 },
      { service: 'IAM', p50: 32, p95: 98, p99: 178 },
      { service: 'Business', p50: 78, p95: 245, p99: 456 },
      { service: 'Notification', p50: 23, p95: 67, p99: 124 },
      { service: 'Payment', p50: 56, p95: 167, p99: 312 },
      { service: 'Analytics', p50: 124, p95: 456, p99: 789 }
    ]);

    // Initialize database metrics
    this.databaseMetrics.set([
      {
        name: 'Primary DB',
        connections: 87,
        maxConnections: 100,
        activeQueries: 12,
        slowQueries: 3,
        cacheHitRate: 94.5
      },
      {
        name: 'Replica DB',
        connections: 45,
        maxConnections: 100,
        activeQueries: 8,
        slowQueries: 1,
        cacheHitRate: 96.2
      }
    ]);

    // Initialize Redis metrics
    this.redisMetrics.set({
      connections: 234,
      memoryUsed: 1.8,
      memoryTotal: 4.0,
      hitRate: 98.7,
      evictions: 123,
      opsPerSec: 15234
    });

    // Initialize slow endpoints
    this.slowEndpoints.set([
      {
        method: 'POST',
        path: '/api/analytics/report',
        avgLatency: 1245,
        p95Latency: 2345,
        calls: 1234
      },
      {
        method: 'GET',
        path: '/api/business/dashboard',
        avgLatency: 876,
        p95Latency: 1567,
        calls: 5678
      },
      {
        method: 'POST',
        path: '/api/payment/process',
        avgLatency: 654,
        p95Latency: 1234,
        calls: 3456
      },
      {
        method: 'GET',
        path: '/api/users/search',
        avgLatency: 543,
        p95Latency: 987,
        calls: 8901
      },
      {
        method: 'PUT',
        path: '/api/business/update',
        avgLatency: 432,
        p95Latency: 876,
        calls: 2345
      }
    ]);
  }

  private startRealtimeUpdates(): void {
    // Simulate real-time updates every 3 seconds
    setInterval(() => {
      const current = this.realtimeMetrics();
      this.realtimeMetrics.set({
        ccu: Math.floor(current.ccu + (Math.random() - 0.5) * 100),
        rps: Math.floor(current.rps + (Math.random() - 0.5) * 200),
        errorRate: Math.max(0, Math.min(5, current.errorRate + (Math.random() - 0.5) * 0.5)),
        avgLatency: Math.floor(current.avgLatency + (Math.random() - 0.5) * 20)
      });

      // Update service metrics
      const services = this.services();
      this.services.set(services.map(s => ({
        ...s,
        cpu: Math.max(0, Math.min(100, s.cpu + (Math.random() - 0.5) * 5)),
        memory: Math.max(0, Math.min(100, s.memory + (Math.random() - 0.5) * 3)),
        requests: s.requests + Math.floor(Math.random() * 100),
        errors: s.errors + Math.floor(Math.random() * 2)
      })));

      // Add new traffic data point
      const history = this.trafficHistory();
      const newPoint: TrafficData = {
        timestamp: new Date(),
        requests: Math.floor(3000 + Math.random() * 2000),
        errors: Math.floor(5 + Math.random() * 20)
      };

      // Keep only last 24 data points
      const updated = [...history.slice(1), newPoint];
      this.trafficHistory.set(updated);
    }, 3000);
  }
}
