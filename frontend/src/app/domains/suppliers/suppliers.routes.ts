import { Routes } from '@angular/router';

export const SUPPLIER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/supplier-list/supplier-list').then((m) => m.SupplierList),
  },
];
