import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

/**
 * Application Routes
 *
 * Route Structure:
 * - / → Redirect to dashboard (if authenticated) or login
 * - /login → Public login page
 * - /auth/callback → OAuth2 callback handler
 * - /dashboard → Protected dashboard (requires auth)
 * - /users → Protected users page (requires auth)
 * - /organizations → Protected organizations page (requires auth)
 * - /settings → Protected settings page (requires auth)
 *
 * All routes use lazy loading for optimal performance
 */
export const routes: Routes = [
  // Default route - redirect to dashboard (requires auth)
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },

  // Public routes (no auth required)
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component')
      .then(m => m.LoginComponent),
    title: 'Login - Enterprise Microservices'
  },

  {
    path: 'auth/callback',
    loadComponent: () => import('./features/auth/callback/callback.component')
      .then(m => m.CallbackComponent),
    title: 'Authenticating...'
  },

  // Protected routes (require authentication)
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component')
      .then(m => m.DashboardComponent),
    canActivate: [authGuard],
    title: 'Dashboard - Enterprise Microservices'
  },

  {
    path: 'users',
    loadChildren: () => import('./features/users/users.routes')
      .then(m => m.USERS_ROUTES),
    canActivate: [authGuard],
    title: 'Users - Enterprise Microservices'
  },

  {
    path: 'organizations',
    loadChildren: () => import('./features/organizations/organizations.routes')
      .then(m => m.ORGANIZATIONS_ROUTES),
    canActivate: [authGuard],
    title: 'Organizations - Enterprise Microservices'
  },

  {
    path: 'customers',
    loadComponent: () => import('./features/customers/customers.component')
      .then(m => m.CustomersComponent),
    canActivate: [authGuard],
    title: 'Customers - Enterprise Microservices'
  },

  {
    path: 'products',
    loadComponent: () => import('./features/products/product-list/product-list.component')
      .then(m => m.ProductListComponent),
    canActivate: [authGuard],
    title: 'Products - Enterprise Microservices'
  },

  {
    path: 'products/:id',
    loadComponent: () => import('./features/products/product-detail/product-detail.component')
      .then(m => m.ProductDetailComponent),
    canActivate: [authGuard],
    title: 'Product Details - Enterprise Microservices'
  },

  {
    path: 'settings',
    loadComponent: () => import('./features/settings/settings.component')
      .then(m => m.SettingsComponent),
    canActivate: [authGuard],
    title: 'Settings - Enterprise Microservices'
  },

  // Wildcard route - 404 page
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
