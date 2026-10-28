import { Routes } from '@angular/router';
import { authGuard } from '@shared/auth/auth.guard';
import { personaGuard, redirectToPersonaHome } from '@shared/persona/persona.guard';
import { Shell } from '@shell/layout/shell';

export const routes: Routes = [
  // Sign in and sign up: no top bar, no session needed.
  {
    path: 'auth',
    loadChildren: () => import('@domains/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: redirectToPersonaHome },

      // Administrator
      {
        path: 'suppliers',
        canActivate: [personaGuard('ADMIN')],
        loadChildren: () => import('@domains/suppliers/suppliers.routes').then((m) => m.SUPPLIER_ROUTES),
      },
      {
        path: 'retailers',
        canActivate: [personaGuard('ADMIN')],
        loadChildren: () => import('@domains/retailers/retailers.routes').then((m) => m.RETAILER_ROUTES),
      },
      {
        path: 'partnerships',
        canActivate: [personaGuard('ADMIN')],
        loadChildren: () => import('@domains/partnerships/partnerships.routes').then((m) => m.PARTNERSHIP_ROUTES),
      },
      {
        path: 'audit-log',
        canActivate: [personaGuard('ADMIN')],
        loadChildren: () => import('@domains/audit/audit.routes').then((m) => m.AUDIT_ROUTES),
      },

      // Supplier
      {
        path: 'my-offers',
        canActivate: [personaGuard('SUPPLIER')],
        loadChildren: () => import('@domains/offers/offers.routes').then((m) => m.OFFER_ROUTES),
      },

      // Retailer
      {
        path: 'offers',
        canActivate: [personaGuard('RETAILER')],
        loadChildren: () => import('@domains/subscriptions/subscriptions.routes').then((m) => m.AVAILABLE_OFFER_ROUTES),
      },
      {
        path: 'subscriptions',
        canActivate: [personaGuard('RETAILER')],
        loadChildren: () => import('@domains/subscriptions/subscriptions.routes').then((m) => m.SUBSCRIPTION_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
