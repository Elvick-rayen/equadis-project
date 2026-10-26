import { computed, effect, Injectable, signal } from '@angular/core';
import { readStorage, writeStorage } from '../storage/local-storage';
import { ADMIN_PERSONA, Persona, PERSONA_HOME, personaFromKey, personaKey } from './persona.model';

const STORAGE_KEY = 'equadis.persona';

/** The current "Acting as" persona. Stands in for authentication until roles exist on the backend. */
@Injectable({ providedIn: 'root' })
export class PersonaStore {
  private readonly _persona = signal<Persona>(this.initialPersona());

  readonly persona = this._persona.asReadonly();
  readonly role = computed(() => this._persona().role);
  readonly key = computed(() => personaKey(this._persona()));
  readonly homeRoute = computed(() => PERSONA_HOME[this.role()]);

  /** Id of the supplier being acted as, or null for other personas. */
  readonly supplierId = computed(() => {
    const persona = this._persona();
    return persona.role === 'SUPPLIER' ? persona.supplierId : null;
  });

  /** Id of the retailer being acted as, or null for other personas. */
  readonly retailerId = computed(() => {
    const persona = this._persona();
    return persona.role === 'RETAILER' ? persona.retailerId : null;
  });

  constructor() {
    effect(() => writeStorage(STORAGE_KEY, this.key()));
  }

  actAs(persona: Persona): void {
    this._persona.set(persona);
  }

  reset(): void {
    this._persona.set(ADMIN_PERSONA);
  }

  private initialPersona(): Persona {
    const stored = readStorage<string>(STORAGE_KEY);
    return (stored && personaFromKey(stored)) || ADMIN_PERSONA;
  }
}
