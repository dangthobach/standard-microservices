import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { AuthService } from '../../core/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * Dashboard Component
 *
 * Main landing page after authentication
 *
 * Features:
 * - Welcome message with username
 * - System statistics cards
 * - Quick action buttons
 * - Test API call to verify authentication
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule
  ],
  template: `
    <div class="dashboard-container">
      <h1>Welcome, {{ username }}!</h1>
      <p class="subtitle">Enterprise Microservices Dashboard</p>

      <mat-grid-list cols="3" rowHeight="200px" gutterSize="20">
        <!-- Users Card -->
        <mat-grid-tile>
          <mat-card class="stat-card">
            <mat-card-header>
              <mat-icon class="card-icon">people</mat-icon>
              <mat-card-title>Users</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <h2 class="stat-number">{{ userCount }}</h2>
              <p>Total registered users</p>
            </mat-card-content>
            <mat-card-actions>
              <button mat-button color="primary">View All</button>
            </mat-card-actions>
          </mat-card>
        </mat-grid-tile>

        <!-- Organizations Card -->
        <mat-grid-tile>
          <mat-card class="stat-card">
            <mat-card-header>
              <mat-icon class="card-icon">business</mat-icon>
              <mat-card-title>Organizations</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <h2 class="stat-number">{{ orgCount }}</h2>
              <p>Active organizations</p>
            </mat-card-content>
            <mat-card-actions>
              <button mat-button color="primary">View All</button>
            </mat-card-actions>
          </mat-card>
        </mat-grid-tile>

        <!-- API Status Card -->
        <mat-grid-tile>
          <mat-card class="stat-card">
            <mat-card-header>
              <mat-icon class="card-icon">cloud_done</mat-icon>
              <mat-card-title>API Status</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <h2 class="stat-number status-ok">✓ Online</h2>
              <p>All services operational</p>
            </mat-card-content>
            <mat-card-actions>
              <button mat-button color="primary" (click)="testApi()">Test API</button>
            </mat-card-actions>
          </mat-card>
        </mat-grid-tile>
      </mat-grid-list>

      <!-- User Info Card -->
      <mat-card class="user-info-card">
        <mat-card-header>
          <mat-card-title>Your Account</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="info-row">
            <strong>Username:</strong> {{ username }}
          </div>
          <div class="info-row">
            <strong>Email:</strong> {{ email }}
          </div>
          <div class="info-row">
            <strong>Session ID:</strong> {{ sessionId }}
          </div>
          <div class="info-row">
            <strong>Authenticated:</strong> {{ isAuthenticated ? 'Yes' : 'No' }}
          </div>
        </mat-card-content>
      </mat-card>

      <!-- API Test Result -->
      @if (apiTestResult) {
        <mat-card class="api-result-card">
          <mat-card-header>
            <mat-card-title>API Test Result</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <pre>{{ apiTestResult | json }}</pre>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .dashboard-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }

    h1 {
      font-size: 32px;
      font-weight: 300;
      margin-bottom: 8px;
    }

    .subtitle {
      font-size: 16px;
      color: #666;
      margin-bottom: 30px;
    }

    .stat-card {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .card-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #1976d2;
    }

    .stat-number {
      font-size: 48px;
      font-weight: 300;
      margin: 10px 0;
      color: #333;
    }

    .status-ok {
      color: #4caf50;
    }

    .user-info-card {
      margin-top: 20px;
    }

    .info-row {
      padding: 8px 0;
      border-bottom: 1px solid #eee;
    }

    .info-row:last-child {
      border-bottom: none;
    }

    .api-result-card {
      margin-top: 20px;
    }

    pre {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 4px;
      overflow-x: auto;
    }
  `]
})
export class DashboardComponent implements OnInit {
  username: string = '';
  email: string = '';
  sessionId: string = '';
  isAuthenticated: boolean = false;

  userCount: number = 0;
  orgCount: number = 0;

  apiTestResult: any = null;

  constructor(
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadStats();
  }

  private loadUserInfo(): void {
    this.username = this.authService.getUsername() || 'Unknown';
    this.email = this.authService.getEmail() || 'unknown@example.com';
    this.sessionId = this.authService.getSessionId() || 'No session';
    this.isAuthenticated = this.authService.isAuthenticated();
  }

  private loadStats(): void {
    // Mock data - replace with actual API calls
    this.userCount = 150;
    this.orgCount = 25;
  }

  testApi(): void {
    // Test API call to verify authentication works
    const apiUrl = environment.apiUrl.replace('/api', '');

    this.http.get(`${apiUrl}/auth/me`).subscribe({
      next: (response) => {
        this.apiTestResult = response;
        console.log('API test successful:', response);
      },
      error: (error) => {
        this.apiTestResult = { error: error.message };
        console.error('API test failed:', error);
      }
    });
  }
}
