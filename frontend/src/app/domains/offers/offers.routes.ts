import { Routes } from '@angular/router';

export const OFFER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/my-offers/my-offers').then((m) => m.MyOffers),
  },
];
