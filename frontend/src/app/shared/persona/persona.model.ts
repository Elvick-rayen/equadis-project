export type PersonaRole = 'ADMIN' | 'SUPPLIER' | 'RETAILER';

/** Who the user is "acting as". Supplier and retailer personas point to one company. */
export type Persona =
  | { role: 'ADMIN' }
  | { role: 'SUPPLIER'; supplierId: number }
  | { role: 'RETAILER'; retailerId: number };

export const ADMIN_PERSONA: Persona = { role: 'ADMIN' };

/** First page of each persona: where the app lands after signing in or switching. */
export const PERSONA_HOME: Record<PersonaRole, string> = {
  ADMIN: '/suppliers',
  SUPPLIER: '/my-offers',
  RETAILER: '/offers',
};

/** Persona key sent to the act-as API and used as the "Acting as" select value: ADMIN, SUPPLIER:12, RETAILER:3. */
export function personaKey(persona: Persona): string {
  switch (persona.role) {
    case 'ADMIN':
      return 'ADMIN';
    case 'SUPPLIER':
      return `SUPPLIER:${persona.supplierId}`;
    case 'RETAILER':
      return `RETAILER:${persona.retailerId}`;
  }
}
