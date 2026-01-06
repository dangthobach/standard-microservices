export interface RealtimeMetrics {
  ccu: number;
  rps: number;
  errorRate: number;
  avgLatency: number;
}

export interface ServiceHealth {
  name: string;
  status: 'healthy' | 'warning' | 'critical' | 'down';
  cpu: number;
  memory: number;
  uptime: string;
  requests: number;
  errors: number;
  threads: number; // Active threads count
  virtualThreadsEnabled: boolean; // Virtual threads enabled flag
}

export interface TrafficData {
  timestamp: Date;
  requests: number;
  errors: number;
}

export interface LatencyData {
  service: string;
  p50: number;
  p95: number;
  p99: number;
}

export interface DatabaseMetrics {
  name: string;
  connections: number;
  maxConnections: number;
  activeQueries: number;
  slowQueries: number;
  cacheHitRate: number;
}

export interface RedisMetrics {
  connections: number;
  memoryUsed: number;
  memoryTotal: number;
  hitRate: number;
  evictions: number;
  opsPerSec: number;
}

export interface L1CacheMetrics {
  cacheName: string;
  hitRate: number; // Hit rate as percentage (0-100)
  hitCount: number; // Total cache hits
  missCount: number; // Total cache misses
  size: number; // Current number of entries
  evictions: number; // Total evictions
  loadCount: number; // Total loads
  loadTime: number; // Total load time in nanoseconds
}

export interface SlowEndpoint {
  method: string;
  path: string;
  avgLatency: number;
  p95Latency: number;
  calls: number;
}

export interface ChartDataPoint {
  name: string;
  value: number;
}

export interface TimeSeriesData {
  name: string;
  series: { name: string; value: number }[];
}
