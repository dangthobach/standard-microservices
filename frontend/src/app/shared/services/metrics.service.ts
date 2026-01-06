import { Injectable, signal } from '@angular/core';
import {
  RealtimeMetrics,
  ServiceHealth,
  TrafficData,
  LatencyData,
  DatabaseMetrics,
  RedisMetrics,
  L1CacheMetrics,
  SlowEndpoint,
  TimeSeriesData
} from '../models/metrics.model';
import { MetricsApiService } from './metrics-api.service';

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
  private l1CacheMetrics = signal<L1CacheMetrics[]>([]);
  private slowEndpoints = signal<SlowEndpoint[]>([]);

  private updateInterval: any;
  private isInitialized = false;

  constructor(private apiService: MetricsApiService) {
    this.loadDataFromApi();
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

  getL1CacheMetrics() {
    return this.l1CacheMetrics.asReadonly();
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

  /**
   * Load all metrics data from backend API
   * Falls back to mock data if API calls fail
   */
  private loadDataFromApi(): void {
    // Load real-time metrics
    this.apiService.getRealtimeMetrics().subscribe({
      next: (metrics) => this.realtimeMetrics.set(metrics),
      error: (error) => {
        console.error('Failed to load real-time metrics, using defaults', error);
        this.realtimeMetrics.set({ ccu: 0, rps: 0, errorRate: 0, avgLatency: 0 });
      }
    });

    // Load service health
    this.apiService.getServiceHealth().subscribe({
      next: (services) => this.services.set(services),
      error: (error) => {
        console.error('Failed to load service health, using defaults', error);
        this.services.set([]);
      }
    });

    // Load traffic history
    this.apiService.getTrafficHistory(24).subscribe({
      next: (traffic) => this.trafficHistory.set(traffic),
      error: (error) => {
        console.error('Failed to load traffic history, using defaults', error);
        this.trafficHistory.set([]);
      }
    });

    // Load latency metrics
    this.apiService.getLatencyMetrics().subscribe({
      next: (latency) => this.latencyMetrics.set(latency),
      error: (error) => {
        console.error('Failed to load latency metrics, using defaults', error);
        this.latencyMetrics.set([]);
      }
    });

    // Load database metrics
    this.apiService.getDatabaseMetrics().subscribe({
      next: (db) => this.databaseMetrics.set(db),
      error: (error) => {
        console.error('Failed to load database metrics, using defaults', error);
        this.databaseMetrics.set([]);
      }
    });

    // Load Redis metrics
    this.apiService.getRedisMetrics().subscribe({
      next: (redis) => this.redisMetrics.set(redis),
      error: (error) => {
        console.error('Failed to load Redis metrics, using defaults', error);
        this.redisMetrics.set(null);
      }
    });

    // Load slow endpoints
    this.apiService.getSlowEndpoints(5).subscribe({
      next: (endpoints) => this.slowEndpoints.set(endpoints),
      error: (error) => {
        console.error('Failed to load slow endpoints, using defaults', error);
        this.slowEndpoints.set([]);
      }
    });

    // L1 Cache metrics - No backend endpoint yet, use mock data
    // TODO: Add backend endpoint for L1 cache metrics
    this.initializeL1CacheMetrics();
    
    this.isInitialized = true;
  }

  /**
   * Initialize L1 Cache metrics (mock data until backend endpoint is available)
   */
  private initializeL1CacheMetrics(): void {
    this.l1CacheMetrics.set([
      {
        cacheName: 'products',
        hitRate: 92.5,
        hitCount: 125432,
        missCount: 10234,
        size: 8456,
        evictions: 1234,
        loadCount: 10234,
        loadTime: 234567890
      },
      {
        cacheName: 'users',
        hitRate: 88.3,
        hitCount: 98765,
        missCount: 12987,
        size: 5678,
        evictions: 890,
        loadCount: 12987,
        loadTime: 345678901
      }
    ]);
  }

  /**
   * Start real-time updates by polling backend API every 5 seconds
   */
  private startRealtimeUpdates(): void {
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
    }

    // Poll backend API every 5 seconds for real-time updates
    this.updateInterval = setInterval(() => {
      if (!this.isInitialized) {
        return; // Wait for initial load
      }

      // Update real-time metrics
      this.apiService.getRealtimeMetrics().subscribe({
        next: (metrics) => this.realtimeMetrics.set(metrics),
        error: (error) => console.warn('Failed to update real-time metrics', error)
      });

      // Update service health
      this.apiService.getServiceHealth().subscribe({
        next: (services) => this.services.set(services),
        error: (error) => console.warn('Failed to update service health', error)
      });

      // Update traffic history (every 30 seconds to reduce load)
      if (Date.now() % 30000 < 5000) {
        this.apiService.getTrafficHistory(24).subscribe({
          next: (traffic) => this.trafficHistory.set(traffic),
          error: (error) => console.warn('Failed to update traffic history', error)
        });
      }

      // Update L1 cache metrics (mock data with variance until backend endpoint is available)
      const l1Cache = this.l1CacheMetrics();
      this.l1CacheMetrics.set(l1Cache.map(cache => ({
        ...cache,
        hitRate: Math.max(80, Math.min(99, cache.hitRate + (Math.random() - 0.5) * 2)),
        hitCount: cache.hitCount + Math.floor(Math.random() * 100),
        missCount: cache.missCount + Math.floor(Math.random() * 10),
        size: Math.max(0, Math.min(10000, cache.size + Math.floor((Math.random() - 0.5) * 50)))
      })));
    }, 5000); // Update every 5 seconds
  }

  /**
   * Manually refresh all metrics from API
   */
  refreshAll(): void {
    this.loadDataFromApi();
  }
}
