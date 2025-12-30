import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { BehaviorSubject, Observable, filter, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * Authentication Service
 *
 * Responsibilities:
 * - Configure OAuth2 PKCE flow with Keycloak
 * - Handle login/logout
 * - Exchange OAuth2 tokens for SESSION_ID
 * - Store SESSION_ID
 * - Provide authentication state
 * - Auto-refresh tokens
 *
 * Flow:
 * 1. User clicks Login
 * 2. Redirect to Keycloak (PKCE flow)
 * 3. User authenticates at Keycloak
 * 4. Keycloak redirects back with authorization code
 * 5. Library exchanges code for access token (with PKCE verifier)
 * 6. POST access token to Gateway /auth/session
 * 7. Gateway validates JWT and creates SESSION_ID
 * 8. Store SESSION_ID locally
 * 9. Use SESSION_ID for subsequent API calls
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authConfig: AuthConfig = {
    issuer: environment.keycloak.issuer,
    redirectUri: window.location.origin + '/auth/callback',
    clientId: environment.keycloak.clientId,
    responseType: 'code',  // Authorization Code flow
    scope: environment.keycloak.scope,
    showDebugInformation: environment.keycloak.showDebugInformation,
    requireHttps: environment.keycloak.requireHttps,

    // PKCE Configuration (automatic with responseType: 'code')
    oidc: true,
    useSilentRefresh: true,
    silentRefreshRedirectUri: window.location.origin + '/assets/silent-refresh.html',
    sessionChecksEnabled: true,

    // Token refresh
    timeoutFactor: 0.75,  // Refresh when 75% of token lifetime passed
  };

  private isAuthenticatedSubject$ = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject$.asObservable();

  private sessionId: string | null = null;

  constructor(
    private oauthService: OAuthService,
    private router: Router,
    private http: HttpClient
  ) {
    this.configureOAuth();
    this.setupAutomaticRefresh();
  }

  /**
   * Configure OAuth2 service
   */
  private configureOAuth(): void {
    this.oauthService.configure(this.authConfig);

    // Load discovery document (OpenID Configuration)
    this.oauthService.loadDiscoveryDocumentAndTryLogin().then(success => {
      if (success) {
        console.log('OAuth2 login successful');
        this.handleSuccessfulLogin();
      } else {
        console.log('No active OAuth2 session');
        this.checkSessionId();
      }
    });

    // Listen to token events
    this.oauthService.events
      .pipe(filter((e: any) => e.type === 'token_received'))
      .subscribe(() => {
        console.log('Access token received');
        this.handleSuccessfulLogin();
      });

    this.oauthService.events
      .pipe(filter((e: any) => e.type === 'token_expires'))
      .subscribe(() => {
        console.warn('Access token expiring soon, refreshing...');
      });

    this.oauthService.events
      .pipe(filter((e: any) => e.type === 'session_terminated'))
      .subscribe(() => {
        console.warn('Session terminated');
        this.handleLogout();
      });
  }

  /**
   * Setup automatic token refresh
   */
  private setupAutomaticRefresh(): void {
    this.oauthService.setupAutomaticSilentRefresh();
  }

  /**
   * Initiate login flow
   *
   * Redirects to Keycloak login page
   */
  login(): void {
    // Store current URL to redirect back after login
    const redirectUrl = this.router.url;
    if (redirectUrl !== '/login' && redirectUrl !== '/auth/callback') {
      localStorage.setItem('redirect_url', redirectUrl);
    }

    this.oauthService.initCodeFlow();
  }

  /**
   * Handle successful login
   *
   * Exchange OAuth2 tokens for SESSION_ID
   */
  private async handleSuccessfulLogin(): Promise<void> {
    if (this.oauthService.hasValidAccessToken()) {
      try {
        // Exchange access token for SESSION_ID
        const sessionId = await this.createSession();

        if (sessionId) {
          this.sessionId = sessionId;
          localStorage.setItem('SESSION_ID', sessionId);
          this.isAuthenticatedSubject$.next(true);

          // Redirect to stored URL or dashboard
          const redirectUrl = localStorage.getItem('redirect_url') || '/dashboard';
          localStorage.removeItem('redirect_url');
          this.router.navigate([redirectUrl]);
        }
      } catch (error) {
        console.error('Failed to create session', error);
        this.logout();
      }
    }
  }

  /**
   * Create session on Gateway
   *
   * POST access token to /auth/session
   * Returns SESSION_ID
   */
  private async createSession(): Promise<string | null> {
    const accessToken = this.oauthService.getAccessToken();
    const refreshToken = this.oauthService.getRefreshToken();

    if (!accessToken) {
      return null;
    }

    try {
      const response = await this.http.post<{sessionId: string, message: string}>(
        `${environment.apiUrl.replace('/api', '')}/auth/session`,
        {
          accessToken,
          refreshToken
        },
        { withCredentials: true }  // Send cookies
      ).toPromise();

      console.log('Session created:', response?.sessionId);
      return response?.sessionId || null;

    } catch (error) {
      console.error('Failed to create session', error);
      throw error;
    }
  }

  /**
   * Check if SESSION_ID exists (for page refresh)
   */
  private checkSessionId(): void {
    const sessionId = localStorage.getItem('SESSION_ID');

    if (sessionId) {
      this.sessionId = sessionId;

      // Verify session with Gateway
      this.http.get(`${environment.apiUrl.replace('/api', '')}/auth/me`, {
        withCredentials: true
      }).subscribe({
        next: (user: any) => {
          if (user.authenticated) {
            this.isAuthenticatedSubject$.next(true);
          } else {
            this.clearSession();
          }
        },
        error: () => {
          this.clearSession();
        }
      });
    }
  }

  /**
   * Logout
   *
   * 1. Clear local session
   * 2. Call Gateway /auth/logout
   * 3. Redirect to Keycloak logout
   */
  logout(): void {
    // Clear session on Gateway
    if (this.sessionId) {
      this.http.post(
        `${environment.apiUrl.replace('/api', '')}/auth/logout`,
        {},
        { withCredentials: true }
      ).subscribe();
    }

    this.handleLogout();

    // Redirect to Keycloak logout
    this.oauthService.logOut();
  }

  /**
   * Handle logout (clear local state)
   */
  private handleLogout(): void {
    this.clearSession();
    this.isAuthenticatedSubject$.next(false);
    this.router.navigate(['/login']);
  }

  /**
   * Clear session
   */
  private clearSession(): void {
    this.sessionId = null;
    localStorage.removeItem('SESSION_ID');
    localStorage.removeItem('redirect_url');
  }

  /**
   * Get access token
   */
  getAccessToken(): string | null {
    return this.oauthService.getAccessToken();
  }

  /**
   * Get SESSION_ID
   */
  getSessionId(): string | null {
    return this.sessionId || localStorage.getItem('SESSION_ID');
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken() || !!this.getSessionId();
  }

  /**
   * Get user claims from ID token
   */
  getUserClaims(): any {
    return this.oauthService.getIdentityClaims();
  }

  /**
   * Get username
   */
  getUsername(): string | null {
    const claims: any = this.getUserClaims();
    return claims?.preferred_username || claims?.email || null;
  }

  /**
   * Get user email
   */
  getEmail(): string | null {
    const claims: any = this.getUserClaims();
    return claims?.email || null;
  }

  /**
   * Refresh access token
   */
  async refreshToken(): Promise<void> {
    try {
      await this.oauthService.refreshToken();
      console.log('Token refreshed successfully');
    } catch (error) {
      console.error('Failed to refresh token', error);
      this.logout();
    }
  }
}
