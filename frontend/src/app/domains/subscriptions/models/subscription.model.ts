export type DiscountType = 'PERCENTAGE_DISCOUNT' | 'FIXED_AMOUNT_DISCOUNT';

/** An offer a retailer can see and subscribe to today (BR-10). */
export interface AvailableOffer {
  id: number;
  supplierId: number;
  supplierName: string;
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

/** BR-12: SUSPENDED while the partnership with the supplier is not active. */
export type SubscriptionState = 'ACTIVE' | 'SUSPENDED' | 'CANCELLED';

export type CancellationCause = 'RETAILER_REQUEST' | 'OFFER_CLOSED' | 'PARTNERSHIP_TERMINATED';

export interface Subscription {
  id: number;
  offerId: number;
  offerReference: string;
  offerTitle: string;
  supplierId: number;
  supplierName: string;
  discountType: DiscountType | null;
  discountValue: number | null;
  currency: string | null;
  validTo: string | null;
  subscribedOn: string;
  cancelledOn: string | null;
  cancellationCause: CancellationCause | null;
  state: SubscriptionState;
}

/** Tag colour per state. */
export const STATE_SEVERITY: Record<SubscriptionState, 'success' | 'warn' | 'secondary'> = {
  ACTIVE: 'success',
  SUSPENDED: 'warn',
  CANCELLED: 'secondary',
};
