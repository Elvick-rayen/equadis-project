import { computed, inject, Injectable } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { ADMIN_PERSONA, Persona, PERSONA_HOME, personaKey } from './persona.model';

/**
 * The persona the signed-in session acts as, read from the session (the JWT decides it).
 * Administrators change it with AuthService.actAs; nobody else can.
 */
@Injectable({ providedIn: 'root' })
export class PersonaStore {
  private readonly _auth = inject(AuthService);

  readonly persona = computed<Persona>(() => {
    const session = this._auth.session();
    if (!session || session.partyId === null) return ADMIN_PERSONA;
    return session.role === 'SUPPLIER'
      ? { role: 'SUPPLIER', supplierId: session.partyId }
      : { role: 'RETAILER', retailerId: session.partyId };
  });

  readonly role = computed(() => this.persona().role);
  readonly key = computed(() => personaKey(this.persona()));
  readonly homeRoute = computed(() => PERSONA_HOME[this.role()]);
  /** Company name of the supplier or retailer acted as; null for ADMIN. */
  readonly displayName = computed(() => this._auth.session()?.displayName ?? null);

  /** Id of the supplier being acted as, or null for other personas. */
  readonly supplierId = computed(() => {
    const persona = this.persona();
    return persona.role === 'SUPPLIER' ? persona.supplierId : null;
  });

  /** Id of the retailer being acted as, or null for other personas. */
  readonly retailerId = computed(() => {
    const persona = this.persona();
    return persona.role === 'RETAILER' ? persona.retailerId : null;
  });
}
