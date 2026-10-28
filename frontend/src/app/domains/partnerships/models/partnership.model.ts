import { PageRequest } from '@shared/http/api';

/** Computed by the backend for today (BR-05). */
export type PartnershipStatus = 'SCHEDULED' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED';

export const PARTNERSHIP_STATUSES: readonly PartnershipStatus[] = ['SCHEDULED', 'ACTIVE', 'EXPIRED', 'TERMINATED'];

/** What the backend allows today; buttons are shown only for these. */
export type PartnershipAction = 'RENEW' | 'TERMINATE';

export interface Renewal {
  previousEndDate: string;
  newEndDate: string;
  renewedOn: string;
}

export interface Partnership {
  id: number;
  supplierId: number;
  supplierName: string;
  retailerId: number;
  retailerName: string;
  /** ISO dates (yyyy-MM-dd). */
  startDate: string;
  endDate: string;
  status: PartnershipStatus;
  allowedActions: PartnershipAction[];
  terminatedOn: string | null;
  terminationReason: string | null;
  /** Only on the detail endpoint. */
  renewals: Renewal[] | null;
}

export interface PartnershipDraft {
  supplierId: number;
  retailerId: number;
  startDate: string;
  endDate: string;
}

/** A status, or "active and ending within 30 days". */
export type PartnershipFilter = PartnershipStatus | 'EXPIRING';

export const EXPIRING_WITHIN_DAYS = 30;

export type PartnershipSortField = 'startDate' | 'endDate';

export interface PartnershipSearch extends PageRequest<PartnershipSortField> {
  filter: PartnershipFilter | null;
}

/** A supplier or retailer to choose in the "New partnership" dialog. */
export interface CompanyOption {
  id: number;
  name: string;
}

/** Tag colour per status. */
export const STATUS_SEVERITY: Record<PartnershipStatus, 'success' | 'info' | 'warn' | 'danger'> = {
  ACTIVE: 'success',
  SCHEDULED: 'info',
  EXPIRED: 'warn',
  TERMINATED: 'danger',
};
