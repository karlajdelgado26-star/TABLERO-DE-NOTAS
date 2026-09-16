import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Note, NoteCreateRequest, NoteUpdateRequest, NotePositionRequest } from '../../../shared/models/note.model';

@Injectable({
  providedIn: 'root'
})
export class NoteService {
  private API_URL = '/api/notes';

  notesSignal = signal<Note[]>([]);

  constructor(private http: HttpClient) {}

  get notes() {
    return this.notesSignal.asReadonly();
  }

  loadNotes(): Observable<Note[]> {
    return this.http.get<Note[]>(this.API_URL).pipe(
      tap(notes => this.notesSignal.set(notes))
    );
  }

  createNote(request: NoteCreateRequest): Observable<Note> {
    return this.http.post<Note>(this.API_URL, request).pipe(
      tap(newNote => {
        this.notesSignal.update(notes => [...notes, newNote]);
      })
    );
  }

  updateNote(id: number, request: NoteUpdateRequest): Observable<Note> {
    return this.http.put<Note>(`${this.API_URL}/${id}`, request).pipe(
      tap(updatedNote => {
        this.notesSignal.update(notes => 
          notes.map(note => note.id === id ? updatedNote : note)
        );
      })
    );
  }

  updatePosition(id: number, request: NotePositionRequest): Observable<Note> {
    return this.http.patch<Note>(`${this.API_URL}/${id}/position`, request).pipe(
      tap(updatedNote => {
        this.notesSignal.update(notes => 
          notes.map(note => note.id === id ? updatedNote : note)
        );
      })
    );
  }

  deleteNote(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        this.notesSignal.update(notes => notes.filter(note => note.id !== id));
      })
    );
  }
}
