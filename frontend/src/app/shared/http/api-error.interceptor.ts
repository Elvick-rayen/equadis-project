import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ApiError, isApiError } from '../errors/api-error';
import { isApiRequest } from './api';

/**
 * Converts API failures into {@link ApiError}s, so callers only deal with business codes.
 * The backend sends ProblemDetails like { code, detail, fieldErrors }.
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url)) {
    return next(request);
  }
  return next(request).pipe(catchError((error: unknown) => throwError(() => toApiError(error))));
};

function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { code: 'UNKNOWN' };
  }
  if (error.status === 0) {
    return { code: 'NETWORK' };
  }
  const body: unknown = error.error;
  if (isApiError(body)) {
    const problem = body as ApiError & { detail?: string };
    return { code: problem.code, message: problem.detail, fieldErrors: problem.fieldErrors };
  }
  return { code: 'UNKNOWN', message: error.message };
}
