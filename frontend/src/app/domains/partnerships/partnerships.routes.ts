import { Routes } from '@angular/router';

export const PARTNERSHIP_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/partnership-list/partnership-list').then((m) => m.PartnershipList),
  },
];
