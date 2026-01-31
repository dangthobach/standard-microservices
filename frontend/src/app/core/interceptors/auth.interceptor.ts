import { HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { OAuthStorage } from 'angular-oauth2-oidc';
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
  const router = inject(Router);
  const injector = inject(Injector);

  // Use OAuthStorage to get token without injecting OAuthService/AuthService
  const authStorage = inject(OAuthStorage);

  // Headers object to be populated
  const headers: { [key: string]: string } = {};

  // 1. Trace ID - Always add for internal requests (Zipkin B3 propagation)
  if (!req.url.startsWith('http')) {
    headers['X-Trace-Id'] = generateTraceId();
  }

  // 2. Authentication - Add if not skipped
  if (!shouldSkipAuth(req.url)) {
    // Get tokens directly from storage
    const accessToken = authStorage.getItem('access_token');
    const sessionId = localStorage.getItem('SESSION_ID');

    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }

    if (sessionId) {
      headers['X-Session-Id'] = sessionId;
    }
  }

  // 3. Clone request with headers
  // Only set withCredentials if we are sending Auth headers (internal API)
  // or if we strictly mean to send cookies. Original logic implied withCredentials for Auth.
  // We'll keep it simple: if we modified headers, we clone.
  // For internal requests, we usually want cookies (session), so withCredentials=true is generally good.
  const authReq = req.clone({
    setHeaders: headers,
    withCredentials: !req.url.startsWith('http') // Send cookies for internal requests
  });

  // Handle response
  return next(authReq).pipe(
    catchError((error) => {
      // Handle 401 Unauthorized
      if (error.status === 401) {
        console.warn('Unauthorized request, redirecting to login');

        // Lazy load AuthService to avoid circular dependency
        const authService = injector.get(AuthService);

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

/**
 * Generate a random 16-character hex string for Zipkin Trace ID
 */
function generateTraceId(): string {
  const chars = '0123456789abcdef';
  let traceId = '';
  for (let i = 0; i < 16; i++) {
    traceId += chars[Math.floor(Math.random() * 16)];
  }
  return traceId;
}
