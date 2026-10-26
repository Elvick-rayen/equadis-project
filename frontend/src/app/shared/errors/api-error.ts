export interface FieldError {
  field: string;
  message: string;
}

/**
 * Error shape returned by the API: a business error code (e.g. SUPPLIER_NAME_TAKEN)
 * that the UI translates with the `errors.<code>` key.
 */
export interface ApiError {
  code: string;
  message?: string;
  fieldErrors?: FieldError[];
}

export const UNKNOWN_ERROR: ApiError = { code: 'UNKNOWN' };

export function isApiError(value: unknown): value is ApiError {
  return typeof value === 'object' && value !== null && typeof (value as ApiError).code === 'string';
}

export function toApiError(value: unknown): ApiError {
  if (isApiError(value)) return value;
  // Angular resources wrap non-Error failures; the ApiError is then the cause.
  if (value instanceof Error && isApiError(value.cause)) return value.cause;
  return UNKNOWN_ERROR;
}
