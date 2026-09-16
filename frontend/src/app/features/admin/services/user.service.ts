import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserCreateRequest, UserUpdateRequest } from '../../../shared/models/user.model';

/** Administración de usuarios (solo ADMIN). */
@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private readonly API_URL = '/api/users';

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.API_URL);
  }

  createUser(request: UserCreateRequest): Observable<User> {
    return this.http.post<User>(this.API_URL, request);
  }

  updateUser(id: number, request: UserUpdateRequest): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}`, request);
  }

  changePassword(id: number, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/password`, { newPassword });
  }

  /** Activa o desactiva. "Eliminar" un usuario es desactivarlo: sus notas se conservan. */
  setActive(id: number, active: boolean): Observable<User> {
    const params = new HttpParams().set('active', active);
    return this.http.patch<User>(`${this.API_URL}/${id}/status`, null, { params });
  }
}
