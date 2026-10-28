import { PageRequest } from '@shared/http/api';

export type StoreType = 'ONLINE' | 'PHYSICAL' | 'HYBRID';

export const STORE_TYPES: readonly StoreType[] = ['PHYSICAL', 'ONLINE', 'HYBRID'];

export interface RetailerLocation {
  /** ISO 3166-1 alpha-2 country code. */
  countryCode: string;
  /** Set for PHYSICAL and HYBRID stores (BR-02). */
  city: string | null;
  address: string | null;
  /** Set for ONLINE and HYBRID stores (BR-02). */
  website: string | null;
}

export interface Retailer {
  id: number;
  storeName: string;
  storeType: StoreType;
  location: RetailerLocation;
  contactEmail: string;
}

/** Payload of POST and PUT /api/retailers. */
export interface RetailerDraft {
  storeName: string;
  storeType: StoreType;
  countryCode: string;
  city: string | null;
  address: string | null;
  website: string | null;
  contactEmail: string;
}

/** Lightweight view used by pickers such as the "Acting as" selector. */
export interface RetailerSummary {
  id: number;
  storeName: string;
}

export type RetailerSortField = 'storeName' | 'storeType' | 'contactEmail' | 'location.city' | 'location.countryCode';

export interface RetailerSearch extends PageRequest<RetailerSortField> {
  query?: string;
}

/** BR-02: physical and hybrid stores have an address; online and hybrid stores have a website. */
export function needsStreetAddress(type: StoreType): boolean {
  return type !== 'ONLINE';
}

export function needsWebsite(type: StoreType): boolean {
  return type !== 'PHYSICAL';
}
