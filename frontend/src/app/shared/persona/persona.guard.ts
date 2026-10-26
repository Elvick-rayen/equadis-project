import { inject } from '@angular/core';
import { CanActivateFn, RedirectFunction, Router } from '@angular/router';
import { PersonaRole } from './persona.model';
import { PersonaStore } from './persona.store';

/** Only lets the given persona in; anyone else is sent to their own home page. */
export function personaGuard(role: PersonaRole): CanActivateFn {
  return () => {
    const personas = inject(PersonaStore);
    return personas.role() === role || inject(Router).parseUrl(personas.homeRoute());
  };
}

/** Redirects the empty path to the current persona's home page. */
export const redirectToPersonaHome: RedirectFunction = () => inject(PersonaStore).homeRoute();
