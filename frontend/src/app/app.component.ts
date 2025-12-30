import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from './core/services/auth.service';
import { Observable } from 'rxjs';

/**
 * Root Application Component
 *
 * Features:
 * - Top navigation bar with user menu
 * - Side navigation drawer
 * - Authentication state display
 * - Logout functionality
 * - Responsive layout
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatMenuModule
  ],
  template: `
    <div class="app-container">
      @if (isAuthenticated$ | async) {
        <!-- Authenticated Layout -->
        <mat-toolbar color="primary" class="app-toolbar">
          <button mat-icon-button (click)="drawer.toggle()">
            <mat-icon>menu</mat-icon>
          </button>

          <span class="app-title">Enterprise Microservices</span>

          <span class="spacer"></span>

          <!-- User Menu -->
          <button mat-button [matMenuTriggerFor]="userMenu">
            <mat-icon>account_circle</mat-icon>
            <span class="user-name">{{ getUsername() }}</span>
          </button>

          <mat-menu #userMenu="matMenu">
            <button mat-menu-item routerLink="/settings">
              <mat-icon>settings</mat-icon>
              <span>Settings</span>
            </button>
            <button mat-menu-item (click)="logout()">
              <mat-icon>logout</mat-icon>
              <span>Logout</span>
            </button>
          </mat-menu>
        </mat-toolbar>

        <mat-sidenav-container class="app-sidenav-container">
          <mat-sidenav #drawer mode="side" opened class="app-sidenav">
            <mat-nav-list>
              <a mat-list-item routerLink="/dashboard" routerLinkActive="active">
                <mat-icon matListItemIcon>dashboard</mat-icon>
                <span matListItemTitle>Dashboard</span>
              </a>

              <a mat-list-item routerLink="/users" routerLinkActive="active">
                <mat-icon matListItemIcon>people</mat-icon>
                <span matListItemTitle>Users</span>
              </a>

              <a mat-list-item routerLink="/organizations" routerLinkActive="active">
                <mat-icon matListItemIcon>business</mat-icon>
                <span matListItemTitle>Organizations</span>
              </a>

              <a mat-list-item routerLink="/settings" routerLinkActive="active">
                <mat-icon matListItemIcon>settings</mat-icon>
                <span matListItemTitle>Settings</span>
              </a>
            </mat-nav-list>
          </mat-sidenav>

          <mat-sidenav-content class="app-content">
            <router-outlet></router-outlet>
          </mat-sidenav-content>
        </mat-sidenav-container>
      } @else {
        <!-- Public Layout (Login/Callback) -->
        <router-outlet></router-outlet>
      }
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }

    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .app-title {
      font-size: 20px;
      font-weight: 500;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .user-name {
      margin-left: 8px;
    }

    .app-sidenav-container {
      flex: 1;
    }

    .app-sidenav {
      width: 250px;
      border-right: 1px solid rgba(0, 0, 0, 0.12);
    }

    .app-content {
      padding: 20px;
      background-color: #f5f5f5;
    }

    .active {
      background-color: rgba(0, 0, 0, 0.04);
    }
  `]
})
export class AppComponent implements OnInit {
  isAuthenticated$: Observable<boolean>;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
  }

  ngOnInit(): void {
    // AuthService automatically handles initialization
    console.log('App initialized');
  }

  getUsername(): string {
    return this.authService.getUsername() || 'User';
  }

  logout(): void {
    this.authService.logout();
  }
}
