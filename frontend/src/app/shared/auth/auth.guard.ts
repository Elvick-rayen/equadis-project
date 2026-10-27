import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PersonaStore } from '../persona/persona.store';
import { AuthService } from './auth.service';

/** Signed-in users only; everyone else goes to the login page. */
export const authGuard: CanActivateFn = () =>
  inject(AuthService).isAuthenticated() || inject(Router).parseUrl('/auth/login');

/** Login and sign-up pages: a signed-in user is sent to their home page instead. */
export const guestGuard: CanActivateFn = () =>
  !inject(AuthService).isAuthenticated() || inject(Router).parseUrl(inject(PersonaStore).homeRoute());
