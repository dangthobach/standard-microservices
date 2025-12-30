import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Auth Guard
 *
 * Protects routes that require authentication
 *
 * Usage:
 * ```typescript
 * {
 *   path: 'dashboard',
 *   component: DashboardComponent,
 *   canActivate: [authGuard]
 * }
 * ```
 *
 * Behavior:
 * - If authenticated: Allow access
 * - If not authenticated: Redirect to /login and store attempted URL
 */
export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Store the attempted URL for redirecting after login
  localStorage.setItem('redirect_url', state.url);

  // Redirect to login page
  router.navigate(['/login']);
  return false;
};
