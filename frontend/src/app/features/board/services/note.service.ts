import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Note, NoteCreateRequest, NoteUpdateRequest, NotePositionRequest } from '../../../shared/models/note.model';

@Injectable({
  providedIn: 'root'
})
export class NoteService {
  private http = inject(HttpClient);

  /** Notas del tablero abierto actualmente. */
  private notesSignal = signal<Note[]>([]);
  readonly notes = this.notesSignal.asReadonly();

  clear() {
    this.notesSignal.set([]);
  }

  loadNotes(boardId: number): Observable<Note[]> {
    return this.http.get<Note[]>(`/api/boards/${boardId}/notes`).pipe(
      tap(notes => this.notesSignal.set(notes))
    );
  }

  createNote(boardId: number, request: NoteCreateRequest): Observable<Note> {
    return this.http.post<Note>(`/api/boards/${boardId}/notes`, request).pipe(
      tap(newNote => {
        this.notesSignal.update(notes => [...notes, newNote]);
      })
    );
  }

  updateNote(id: number, request: NoteUpdateRequest): Observable<Note> {
    return this.http.put<Note>(`/api/notes/${id}`, request).pipe(
      tap(updatedNote => this.replaceNote(updatedNote))
    );
  }

  updatePosition(id: number, request: NotePositionRequest): Observable<Note> {
    return this.http.patch<Note>(`/api/notes/${id}/position`, request).pipe(
      tap(updatedNote => this.replaceNote(updatedNote))
    );
  }

  deleteNote(id: number): Observable<void> {
    return this.http.delete<void>(`/api/notes/${id}`).pipe(
      tap(() => {
        this.notesSignal.update(notes => notes.filter(note => note.id !== id));
      })
    );
  }

  private replaceNote(updatedNote: Note) {
    this.notesSignal.update(notes =>
      notes.map(note => note.id === updatedNote.id ? updatedNote : note)
    );
  }
}
