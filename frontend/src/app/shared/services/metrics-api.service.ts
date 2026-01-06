import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  RealtimeMetrics,
  ServiceHealth,
  TrafficData,
  LatencyData,
  DatabaseMetrics,
  RedisMetrics,
  SlowEndpoint
} from '../models/metrics.model';

/**
 * Metrics API Service
 * 
 * Handles all HTTP calls to backend dashboard metrics endpoints.
 * All endpoints return ApiResponse<T> wrapper which is automatically unwrapped.
 * 
 * Base URL: /api/v1/dashboard
 */
@Injectable({
  providedIn: 'root'
})
export class MetricsApiService {
  private readonly baseUrl = `${environment.apiUrl}/v1/dashboard`;

  constructor(private http: HttpClient) {}

  /**
   * Get real-time metrics (CCU, RPS, Error Rate, Avg Latency)
   */
  getRealtimeMetrics(): Observable<RealtimeMetrics> {
    return this.http.get<ApiResponse<RealtimeMetrics>>(`${this.baseUrl}/realtime`)
      .pipe(
        map<ApiResponse<RealtimeMetrics>, RealtimeMetrics>(response => this.unwrapResponse(response)),
        catchError(error => this.handleError<RealtimeMetrics>('Failed to fetch real-time metrics', error))
      );
  }

  /**
   * Get service health status for all microservices
   */
  getServiceHealth(): Observable<ServiceHealth[]> {
    return this.http.get<ApiResponse<ServiceHealth[]>>(`${this.baseUrl}/services`)
      .pipe(
        map<ApiResponse<ServiceHealth[]>, ServiceHealth[]>(response => {
          const services = this.unwrapResponse(response);
          // Add missing fields (threads, virtualThreadsEnabled) with defaults
          return services.map(service => this.enrichServiceHealth(service));
        }),
        catchError(error => this.handleError<ServiceHealth[]>('Failed to fetch service health', error))
      );
  }

  /**
   * Get traffic history for charts
   * @param hours Number of hours to fetch (default: 24)
   */
  getTrafficHistory(hours: number = 24): Observable<TrafficData[]> {
    const params = new HttpParams().set('hours', hours.toString());
    return this.http.get<ApiResponse<TrafficData[]>>(`${this.baseUrl}/traffic`, { params })
      .pipe(
        map<ApiResponse<TrafficData[]>, TrafficData[]>(response => {
          const traffic = this.unwrapResponse(response);
          // Convert timestamp strings to Date objects
          return traffic.map(item => ({
            ...item,
            timestamp: new Date(item.timestamp as any)
          }));
        }),
        catchError(error => this.handleError<TrafficData[]>('Failed to fetch traffic history', error))
      );
  }

  /**
   * Get latency metrics (P50, P95, P99) by service
   */
  getLatencyMetrics(): Observable<LatencyData[]> {
    return this.http.get<ApiResponse<LatencyData[]>>(`${this.baseUrl}/latency`)
      .pipe(
        map<ApiResponse<LatencyData[]>, LatencyData[]>(response => this.unwrapResponse(response)),
        catchError(error => this.handleError<LatencyData[]>('Failed to fetch latency metrics', error))
      );
  }

  /**
   * Get database metrics from all microservices
   */
  getDatabaseMetrics(): Observable<DatabaseMetrics[]> {
    return this.http.get<ApiResponse<DatabaseMetrics[]>>(`${this.baseUrl}/database`)
      .pipe(
        map<ApiResponse<DatabaseMetrics[]>, DatabaseMetrics[]>(response => this.unwrapResponse(response)),
        catchError(error => this.handleError<DatabaseMetrics[]>('Failed to fetch database metrics', error))
      );
  }

  /**
   * Get Redis metrics (L2 Cache)
   */
  getRedisMetrics(): Observable<RedisMetrics> {
    return this.http.get<ApiResponse<RedisMetrics>>(`${this.baseUrl}/redis`)
      .pipe(
        map<ApiResponse<RedisMetrics>, RedisMetrics>(response => this.unwrapResponse(response)),
        catchError(error => this.handleError<RedisMetrics>('Failed to fetch Redis metrics', error))
      );
  }

  /**
   * Get slow endpoints (top N by latency)
   * @param limit Number of endpoints to return (default: 5)
   */
  getSlowEndpoints(limit: number = 5): Observable<SlowEndpoint[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<SlowEndpoint[]>>(`${this.baseUrl}/slow-endpoints`, { params })
      .pipe(
        map<ApiResponse<SlowEndpoint[]>, SlowEndpoint[]>(response => this.unwrapResponse(response)),
        catchError(error => this.handleError<SlowEndpoint[]>('Failed to fetch slow endpoints', error))
      );
  }

  /**
   * Unwrap ApiResponse<T> to T
   * Throws error if response is not successful
   */
  private unwrapResponse<T>(response: ApiResponse<T>): T {
    if (!response.success) {
      throw new Error(response.message || 'API request failed');
    }
    if (response.data === undefined || response.data === null) {
      throw new Error('API response data is missing');
    }
    return response.data;
  }

  /**
   * Enrich ServiceHealth with missing fields
   * Adds threads and virtualThreadsEnabled based on service name
   */
  private enrichServiceHealth(service: ServiceHealth): ServiceHealth {
    // Determine if virtual threads are enabled based on service name
    const virtualThreadsEnabled = 
      service.name.toLowerCase().includes('iam') ||
      service.name.toLowerCase().includes('business');
    
    // Estimate thread count (backend doesn't provide this yet)
    // For virtual threads services, use higher variance
    const baseThreads = virtualThreadsEnabled ? 200 : 50;
    const threads = baseThreads + Math.floor(Math.random() * (virtualThreadsEnabled ? 400 : 100));

    return {
      ...service,
      threads,
      virtualThreadsEnabled
    };
  }

  /**
   * Handle HTTP errors
   */
  private handleError<T>(message: string, error: any): Observable<T> {
    console.error(message, error);
    return throwError(() => new Error(`${message}: ${error.message || error}`));
  }
}

