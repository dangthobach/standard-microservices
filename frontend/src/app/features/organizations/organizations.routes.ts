import { Routes } from '@angular/router';

export const ORGANIZATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./organizations-list/organizations-list.component')
      .then(m => m.OrganizationsListComponent)
  }
];
