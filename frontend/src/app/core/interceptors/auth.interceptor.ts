import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

/**
 * Auth Interceptor
 *
 * Automatically adds authentication headers to API requests:
 * - Authorization: Bearer {access_token}
 * - X-Session-Id: {session_id}
 *
 * Also handles 401 Unauthorized responses by redirecting to login
 *
 * Configuration in app.config.ts:
 * ```typescript
 * provideHttpClient(
 *   withInterceptors([authInterceptor])
 * )
 * ```
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Skip auth header for certain requests
  if (shouldSkipAuth(req.url)) {
    return next(req);
  }

  // Get tokens
  const accessToken = authService.getAccessToken();
  const sessionId = authService.getSessionId();

  // Clone request and add auth headers
  let authReq = req;

  if (accessToken || sessionId) {
    const headers: {[key: string]: string} = {};

    // Add Bearer token
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }

    // Add Session ID
    if (sessionId) {
      headers['X-Session-Id'] = sessionId;
    }

    authReq = req.clone({
      setHeaders: headers,
      withCredentials: true  // Send cookies (SESSION_ID cookie)
    });
  }

  // Handle response
  return next(authReq).pipe(
    catchError((error) => {
      // Handle 401 Unauthorized
      if (error.status === 401) {
        console.warn('Unauthorized request, redirecting to login');

        // Store current URL for redirect after login
        const currentUrl = router.url;
        if (currentUrl !== '/login' && currentUrl !== '/auth/callback') {
          localStorage.setItem('redirect_url', currentUrl);
        }

        // Logout and redirect to login
        authService.logout();
      }

      return throwError(() => error);
    })
  );
};

/**
 * Check if request should skip authentication
 *
 * Skip auth for:
 * - Login/logout endpoints
 * - Public endpoints
 * - External URLs
 */
function shouldSkipAuth(url: string): boolean {
  const skipPatterns = [
    '/auth/session',
    '/auth/login',
    '/auth/logout',
    '/public',
    '/assets',
    'http://',
    'https://'
  ];

  return skipPatterns.some(pattern => url.includes(pattern));
}
