import { PageRequest } from '@shared/http/api';

export interface SupplierContact {
  name: string;
  email: string;
  phone: string;
}

export interface Supplier {
  id: number;
  companyName: string;
  contact: SupplierContact;
  /** ISO 3166-1 alpha-2 country of operation, e.g. "FR". */
  countryCode: string;
}

/** Payload for creating or updating a supplier. */
export type SupplierDraft = Omit<Supplier, 'id'>;

/** Lightweight view used by pickers such as the "Acting as" selector. */
export interface SupplierSummary {
  id: number;
  companyName: string;
}

/** Fields the API can sort on. */
export type SupplierSortField = 'companyName' | 'contact.name' | 'contact.email' | 'countryCode';

/** Table search: page, sort and the optional search text. */
export interface SupplierSearch extends PageRequest<SupplierSortField> {
  query?: string;
}

export const SupplierErrorCode = {
  NameTaken: 'SUPPLIER_NAME_TAKEN',
  NotFound: 'SUPPLIER_NOT_FOUND',
} as const;
