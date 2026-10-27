import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE } from '../http/api';
import { readStorage, removeStorage, writeStorage } from '../storage/local-storage';
import { AuthSession, LoginRequest, SignUpRequest } from './auth.model';

const STORAGE_KEY = 'equadis.session';
const AUTH_URL = `${API_BASE}/auth`;

/** URLs that must go out without a token (and whose 401 means "wrong password", not "expired"). */
export const PUBLIC_AUTH_URLS = [`${AUTH_URL}/login`, `${AUTH_URL}/signup`];

/**
 * The signed-in session (JWT + who it acts as), kept in localStorage so a reload stays signed in.
 * The backend checks the token on every call; this only drives the UI.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _http = inject(HttpClient);

  private readonly _session = signal<AuthSession | null>(this.storedSession());

  readonly session = this._session.asReadonly();
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly token = computed(() => this._session()?.token ?? null);
  readonly canActAs = computed(() => this._session()?.canActAs ?? false);

  constructor() {
    effect(() => {
      const session = this._session();
      if (session) {
        writeStorage(STORAGE_KEY, session);
      } else {
        removeStorage(STORAGE_KEY);
      }
    });
  }

  login(request: LoginRequest): Observable<AuthSession> {
    return this._http.post<AuthSession>(`${AUTH_URL}/login`, request).pipe(tap((session) => this._session.set(session)));
  }

  signUp(request: SignUpRequest): Observable<AuthSession> {
    return this._http.post<AuthSession>(`${AUTH_URL}/signup`, request).pipe(tap((session) => this._session.set(session)));
  }

  /** Administrators only: switch persona (ADMIN, SUPPLIER:<id> or RETAILER:<id>) without signing out. */
  actAs(personaKey: string): Observable<AuthSession> {
    return this._http
      .post<AuthSession>(`${AUTH_URL}/act-as`, { persona: personaKey })
      .pipe(tap((session) => this._session.set(session)));
  }

  logout(): void {
    this._session.set(null);
  }

  private storedSession(): AuthSession | null {
    const session = readStorage<AuthSession>(STORAGE_KEY);
    const expired = !session?.token || Date.parse(session.expiresAt) <= Date.now();
    return expired ? null : session;
  }
}
