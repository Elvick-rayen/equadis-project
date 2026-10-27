import { Routes } from '@angular/router';
import { authGuard } from '@shared/auth/auth.guard';
import { personaGuard, redirectToPersonaHome } from '@shared/persona/persona.guard';
import { ComingSoon } from '@shared/ui/coming-soon/coming-soon';
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
      { path: 'retailers', canActivate: [personaGuard('ADMIN')], component: ComingSoon, data: { titleKey: 'nav.retailers' } },
      { path: 'partnerships', canActivate: [personaGuard('ADMIN')], component: ComingSoon, data: { titleKey: 'nav.partnerships' } },

      // Supplier
      { path: 'my-offers', canActivate: [personaGuard('SUPPLIER')], component: ComingSoon, data: { titleKey: 'nav.myOffers' } },

      // Retailer
      { path: 'offers', canActivate: [personaGuard('RETAILER')], component: ComingSoon, data: { titleKey: 'nav.availableOffers' } },
      { path: 'subscriptions', canActivate: [personaGuard('RETAILER')], component: ComingSoon, data: { titleKey: 'nav.mySubscriptions' } },
    ],
  },
  { path: '**', redirectTo: '' },
];
