import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { MetricsService } from '../../shared/services/metrics.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatDividerModule,
    MatChipsModule,
    MatProgressBarModule,
    NgxChartsModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  // Realtime metrics
  realtimeMetrics = this.metricsService.getRealtimeMetrics();

  // Service health
  services = this.metricsService.getServices();

  // Traffic data
  trafficHistory = this.metricsService.getTrafficHistory();
  trafficChartData = signal(this.metricsService.getTrafficChartData());

  // Latency metrics
  latencyMetrics = this.metricsService.getLatencyMetrics();

  // Database metrics
  databaseMetrics = this.metricsService.getDatabaseMetrics();

  // Redis metrics
  redisMetrics = this.metricsService.getRedisMetrics();

  // Slow endpoints
  slowEndpoints = this.metricsService.getSlowEndpoints();

  // Chart options
  chartColorScheme: any = {
    domain: ['#1976d2', '#f44336', '#4caf50', '#ff9800', '#9c27b0']
  };

  // Responsive chart view - will be calculated based on screen size
  chartView: [number, number] = [700, 300]; // Will be updated based on screen size
  showXAxis = true;
  showYAxis = true;
  showLegend = true;
  showXAxisLabel = true;
  showYAxisLabel = true;
  xAxisLabel = 'Time';
  yAxisLabel = 'Requests';
  timeline = true;
  autoScale = true;

  // Interval for updating chart data
  private updateInterval: any;

  constructor(private metricsService: MetricsService) {}

  ngOnInit(): void {
    // Calculate initial chart size based on window width
    this.calculateChartSize();

    // Listen to window resize for responsive chart
    window.addEventListener('resize', () => this.calculateChartSize());

    // Update chart data every 3 seconds
    this.updateInterval = setInterval(() => {
      this.trafficChartData.set(this.metricsService.getTrafficChartData());
    }, 3000);
  }

  /**
   * Calculate responsive chart dimensions based on window size
   */
  private calculateChartSize(): void {
    const width = window.innerWidth;

    if (width < 768) {
      // Mobile: Full width, smaller height
      this.chartView = [width - 80, 250];
      this.showLegend = false; // Hide legend on mobile
    } else if (width < 1024) {
      // Tablet: Responsive width
      this.chartView = [width - 120, 280];
      this.showLegend = true;
    } else {
      // Desktop: Fixed optimal size
      this.chartView = [700, 300];
      this.showLegend = true;
    }
  }

  ngOnDestroy(): void {
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
    }
    // Clean up resize listener
    window.removeEventListener('resize', () => this.calculateChartSize());
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'healthy':
        return 'status-healthy';
      case 'warning':
        return 'status-warning';
      case 'critical':
        return 'status-critical';
      case 'down':
        return 'status-down';
      default:
        return '';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'healthy':
        return 'check_circle';
      case 'warning':
        return 'warning';
      case 'critical':
        return 'error';
      case 'down':
        return 'cancel';
      default:
        return 'help';
    }
  }

  getMethodColor(method: string): string {
    switch (method) {
      case 'GET':
        return 'method-get';
      case 'POST':
        return 'method-post';
      case 'PUT':
        return 'method-put';
      case 'DELETE':
        return 'method-delete';
      default:
        return '';
    }
  }

  refreshMetrics(): void {
    // Metrics service auto-updates, just update chart
    this.trafficChartData.set(this.metricsService.getTrafficChartData());
  }
}
