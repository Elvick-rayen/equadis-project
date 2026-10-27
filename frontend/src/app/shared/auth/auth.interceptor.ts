import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { toApiError } from '../errors/api-error';
import { isApiRequest } from '../http/api';
import { NotificationService } from '../notifications/notification.service';
import { AuthService, PUBLIC_AUTH_URLS } from './auth.service';

/**
 * Sends the JWT with every API call (Authorization: Bearer ...). When the API answers
 * UNAUTHENTICATED (token missing, expired or invalid), signs out and goes to the login page.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url) || PUBLIC_AUTH_URLS.includes(request.url)) {
    return next(request);
  }
  const auth = inject(AuthService);
  const router = inject(Router);
  const notifications = inject(NotificationService);

  const token = auth.token();
  const authorized = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (toApiError(error).code === 'UNAUTHENTICATED' && auth.isAuthenticated()) {
        auth.logout();
        notifications.error({ code: 'SESSION_EXPIRED' });
        void router.navigateByUrl('/auth/login');
      }
      return throwError(() => error);
    }),
  );
};
