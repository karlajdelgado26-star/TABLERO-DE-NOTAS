import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Board, BoardRequest } from '../../../shared/models/board.model';

@Injectable({
  providedIn: 'root'
})
export class BoardService {
  private http = inject(HttpClient);
  private readonly API_URL = '/api/boards';

  getBoards(): Observable<Board[]> {
    return this.http.get<Board[]>(this.API_URL);
  }

  getBoard(id: number): Observable<Board> {
    return this.http.get<Board>(`${this.API_URL}/${id}`);
  }

  /** Solo ADMIN y LEADER. */
  createBoard(request: BoardRequest): Observable<Board> {
    return this.http.post<Board>(this.API_URL, request);
  }

  /** Solo ADMIN y LEADER. */
  updateBoard(id: number, request: BoardRequest): Observable<Board> {
    return this.http.put<Board>(`${this.API_URL}/${id}`, request);
  }

  /** Solo ADMIN y LEADER. Elimina también las notas del tablero. */
  deleteBoard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
