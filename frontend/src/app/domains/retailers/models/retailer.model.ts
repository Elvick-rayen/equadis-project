export type StoreType = 'ONLINE' | 'PHYSICAL' | 'HYBRID';

export interface RetailerLocation {
  /** ISO 3166-1 alpha-2 country code. */
  countryCode: string;
  /** Required for PHYSICAL and HYBRID stores (BR-02). */
  city?: string;
  address?: string;
  /** Required for ONLINE and HYBRID stores (BR-02). */
  website?: string;
}

export interface Retailer {
  id: number;
  storeName: string;
  storeType: StoreType;
  location: RetailerLocation;
  contactEmail: string;
}

/** Lightweight view used by pickers such as the "Acting as" selector. */
export interface RetailerSummary {
  id: number;
  storeName: string;
}
