import { Routes } from '@angular/router';

export const RETAILER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/retailer-list/retailer-list').then((m) => m.RetailerList),
  },
];
