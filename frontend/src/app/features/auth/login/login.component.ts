import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Login Component
 *
 * Simple login page that initiates OAuth2 PKCE flow
 *
 * Flow:
 * 1. User clicks "Login with Keycloak" button
 * 2. AuthService initiates OAuth2 code flow
 * 3. User redirected to Keycloak
 * 4. After login, redirected to /auth/callback
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Enterprise Microservices</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <div class="login-content">
            <p>Welcome! Please sign in with your credentials.</p>

            @if (loading) {
              <mat-spinner diameter="40"></mat-spinner>
            } @else {
              <button
                mat-raised-button
                color="primary"
                (click)="login()"
                class="login-button">
                Login with Keycloak
              </button>
            }
          </div>
        </mat-card-content>

        <mat-card-footer>
          <p class="footer-text">Powered by Keycloak OAuth2 PKCE</p>
        </mat-card-footer>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
    }

    .login-card {
      max-width: 400px;
      width: 100%;
    }

    .login-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
      padding: 20px 0;
    }

    .login-button {
      min-width: 200px;
    }

    .footer-text {
      text-align: center;
      font-size: 12px;
      color: #666;
      margin: 10px 0;
    }
  `]
})
export class LoginComponent {
  loading = false;

  constructor(private authService: AuthService) {}

  login(): void {
    this.loading = true;
    this.authService.login();
  }
}
