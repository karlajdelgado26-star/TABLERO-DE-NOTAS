import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.models';
import { Role, User } from '../../shared/models/user.model';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private currentUserSignal = signal<User | null>(this.readStoredUser());

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly role = computed<Role | null>(() => this.currentUserSignal()?.role ?? null);

  /** ADMIN: administrar usuarios. */
  readonly canManageUsers = computed(() => this.role() === 'ADMIN');
  /** ADMIN y LEADER: crear, modificar y eliminar tableros. */
  readonly canManageBoards = computed(() => this.role() === 'ADMIN' || this.role() === 'LEADER');

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', credentials).pipe(
      tap(response => {
        localStorage.setItem(TOKEN_KEY, response.token);
        this.storeUser(response.user);
      })
    );
  }

  /** Vuelve a pedir los datos del usuario (por si el administrador cambió su rol o nombre). */
  refreshCurrentUser(): Observable<User> {
    return this.http.get<User>('/api/auth/me').pipe(
      tap(user => this.storeUser(user))
    );
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasAnyRole(roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  private storeUser(user: User) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private readStoredUser(): User | null {
    const userJson = localStorage.getItem(USER_KEY);
    if (!userJson || userJson === 'undefined') {
      localStorage.removeItem(USER_KEY);
      return null;
    }
    try {
      return JSON.parse(userJson) as User;
    } catch (error) {
      console.error('Error parsing user JSON from localStorage', error);
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }
}
