import { PersonaRole } from '../persona/persona.model';

/** Returned by login, sign-up and act-as: the bearer token plus what the UI shows. */
export interface AuthSession {
  token: string;
  /** ISO instant. */
  expiresAt: string;
  /** The account that signed in; stays the admin's while an admin acts as someone else. */
  username: string;
  /** The persona the token acts as. */
  role: PersonaRole;
  /** Supplier or retailer id; null for ADMIN. */
  partyId: number | null;
  /** Company name; null for ADMIN. */
  displayName: string | null;
  /** True for administrators, who may switch persona. */
  canActAs: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export type StoreType = 'ONLINE' | 'PHYSICAL' | 'HYBRID';

export type SignUpRequest =
  | {
      username: string;
      password: string;
      role: 'SUPPLIER';
      supplier: {
        companyName: string;
        contactName: string;
        contactEmail: string;
        contactPhone: string;
        countryCode: string;
      };
    }
  | {
      username: string;
      password: string;
      role: 'RETAILER';
      retailer: {
        storeName: string;
        storeType: StoreType;
        countryCode: string;
        city: string | null;
        address: string | null;
        website: string | null;
        contactEmail: string;
      };
    };
