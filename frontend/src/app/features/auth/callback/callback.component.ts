import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Callback Component
 *
 * Handles OAuth2 redirect after Keycloak authentication
 *
 * Flow:
 * 1. Keycloak redirects to /auth/callback?code=xxx
 * 2. angular-oauth2-oidc library automatically handles code exchange
 * 3. AuthService creates session with Gateway
 * 4. Redirect to originally requested page or dashboard
 *
 * This component just shows a loading spinner during the process
 */
@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="callback-container">
      <mat-spinner></mat-spinner>
      <p>Completing authentication...</p>
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      gap: 20px;
    }
  `]
})
export class CallbackComponent implements OnInit {

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // OAuth2 library automatically handles the callback
    // AuthService will handle session creation and redirect
    console.log('Processing OAuth2 callback...');
  }
}
