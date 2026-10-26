export type PersonaRole = 'ADMIN' | 'SUPPLIER' | 'RETAILER';

/** Who the user is "acting as". Supplier and retailer personas point to one company. */
export type Persona =
  | { role: 'ADMIN' }
  | { role: 'SUPPLIER'; supplierId: number }
  | { role: 'RETAILER'; retailerId: string };

export const ADMIN_PERSONA: Persona = { role: 'ADMIN' };

/** First page of each persona: where the app lands after switching. */
export const PERSONA_HOME: Record<PersonaRole, string> = {
  ADMIN: '/suppliers',
  SUPPLIER: '/my-offers',
  RETAILER: '/offers',
};

/** Stable string key, used as the value of the "Acting as" select and the X-Acting-As header. */
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

export function personaFromKey(key: string): Persona | null {
  const [role, id] = key.split(':');
  if (role === 'ADMIN') return ADMIN_PERSONA;
  if (role === 'SUPPLIER' && /^\d+$/.test(id ?? '')) return { role, supplierId: Number(id) };
  if (role === 'RETAILER' && id) return { role, retailerId: id };
  return null;
}
