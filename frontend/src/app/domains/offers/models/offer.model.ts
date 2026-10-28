import { PageRequest } from '@shared/http/api';

/** BR-08: DRAFT → PUBLISHED → CLOSED. */
export type OfferStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';

export const OFFER_STATUSES: readonly OfferStatus[] = ['DRAFT', 'PUBLISHED', 'CLOSED'];

export type DiscountType = 'PERCENTAGE_DISCOUNT' | 'FIXED_AMOUNT_DISCOUNT';

export const DISCOUNT_TYPES: readonly DiscountType[] = ['PERCENTAGE_DISCOUNT', 'FIXED_AMOUNT_DISCOUNT'];

/** Currencies offered for fixed-amount discounts (any ISO 4217 code is accepted by the API). */
export const CURRENCIES: readonly string[] = ['EUR', 'CHF', 'GBP', 'USD', 'SEK', 'DKK', 'NOK', 'PLN', 'CZK'];

/** What the backend allows now; buttons are shown only for these. */
export type OfferAction = 'EDIT' | 'PUBLISH' | 'CLOSE';

export interface Offer {
  id: number;
  supplierId: number;
  reference: string;
  title: string;
  description: string | null;
  discountType: DiscountType;
  discountValue: number;
  currency: string | null;
  minimumOrderQuantity: number | null;
  /** ISO dates (yyyy-MM-dd). */
  validFrom: string;
  validTo: string;
  status: OfferStatus;
  allowedActions: OfferAction[];
}

/** Payload of POST and PUT /api/suppliers/{id}/offers. */
export interface OfferDraft {
  reference: string;
  title: string;
  description: string | null;
  discountType: DiscountType;
  discountValue: number;
  currency: string | null;
  minimumOrderQuantity: number | null;
  validFrom: string;
  validTo: string;
}

export type OfferSortField = 'reference' | 'title' | 'validFrom' | 'validTo' | 'status';

export interface OfferSearch extends PageRequest<OfferSortField> {
  supplierId: number;
  status: OfferStatus | null;
}

/** Tag colour per status. */
export const OFFER_STATUS_SEVERITY: Record<OfferStatus, 'secondary' | 'success' | 'danger'> = {
  DRAFT: 'secondary',
  PUBLISHED: 'success',
  CLOSED: 'danger',
};
