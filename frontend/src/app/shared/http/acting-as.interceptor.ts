import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { PersonaStore } from '../persona/persona.store';
import { isApiRequest } from './api';

export const ACTING_AS_HEADER = 'X-Acting-As';

/** Tells the backend which persona is calling: ADMIN, SUPPLIER:<id> or RETAILER:<id>. */
export const actingAsInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url)) {
    return next(request);
  }
  const key = inject(PersonaStore).key();
  return next(request.clone({ setHeaders: { [ACTING_AS_HEADER]: key } }));
};
