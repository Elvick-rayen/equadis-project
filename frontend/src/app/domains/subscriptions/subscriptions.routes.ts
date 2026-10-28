import { Routes } from '@angular/router';

export const AVAILABLE_OFFER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/available-offers/available-offers').then((m) => m.AvailableOffers),
  },
];

export const SUBSCRIPTION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/my-subscriptions/my-subscriptions').then((m) => m.MySubscriptions),
  },
];
