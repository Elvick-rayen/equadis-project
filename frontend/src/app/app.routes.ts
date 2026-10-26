import { Routes } from '@angular/router';
import { personaGuard, redirectToPersonaHome } from './shared/persona/persona.guard';
import { ComingSoon } from './shared/ui/coming-soon/coming-soon';
import { Shell } from './shell/layout/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: '', pathMatch: 'full', redirectTo: redirectToPersonaHome },

      // Administrator
      {
        path: 'suppliers',
        canActivate: [personaGuard('ADMIN')],
        loadChildren: () => import('./domains/suppliers/suppliers.routes').then((m) => m.SUPPLIER_ROUTES),
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
