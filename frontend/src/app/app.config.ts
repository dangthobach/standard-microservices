import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

/**
 * Application Configuration
 *
 * Provides:
 * - Router with routes configuration
 * - HTTP client with auth interceptor
 * - OAuth2 client for Keycloak integration
 * - Angular Material animations
 * - Zone.js change detection
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Zone change detection (standard Angular)
    provideZoneChangeDetection({ eventCoalescing: true }),

    // Router
    provideRouter(routes),

    // HTTP Client with Auth Interceptor
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),

    // OAuth2 Client (angular-oauth2-oidc)
    provideOAuthClient(),

    // Angular Material Animations
    provideAnimationsAsync()
  ]
};
