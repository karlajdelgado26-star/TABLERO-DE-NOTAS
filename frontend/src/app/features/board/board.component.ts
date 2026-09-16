import { Component, OnInit, inject, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragEnd, DragDropModule } from '@angular/cdk/drag-drop';
import { NoteService } from './services/note.service';
import { NoteCardComponent } from './components/note-card/note-card.component';
import { AuthService } from '../../core/services/auth.service';
import { Note, NoteCreateRequest, NoteUpdateRequest } from '../../shared/models/note.model';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule, DragDropModule, NoteCardComponent],
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit {
  private noteService = inject(NoteService);
  public authService = inject(AuthService);
  
  notes: Signal<Note[]> = this.noteService.notesSignal;
  errorMessage = '';

  ngOnInit() {
    this.loadNotes();
  }

  loadNotes() {
    this.noteService.loadNotes().subscribe({
      error: () => this.errorMessage = 'Error al cargar las notas.'
    });
  }

  createNote() {
    const newNote: NoteCreateRequest = {
      title: 'Nueva Nota',
      content: '',
      status: 'PENDING',
      positionX: 50,
      positionY: 50
    };
    
    this.noteService.createNote(newNote).subscribe({
      error: () => this.errorMessage = 'Error al crear la nota.'
    });
  }

  onUpdateNote(event: { id: number, request: NoteUpdateRequest }) {
    this.noteService.updateNote(event.id, event.request).subscribe({
      error: () => this.errorMessage = 'Error al actualizar la nota.'
    });
  }

  onDeleteNote(id: number) {
    this.noteService.deleteNote(id).subscribe({
      error: () => this.errorMessage = 'Error al eliminar la nota.'
    });
  }

  onDragEnded(event: CdkDragEnd, note: any) {
    const element = event.source.getRootElement();
    const boundingClientRect = element.getBoundingClientRect();
    const parentPosition = this.getBoardOffset();

    const newX = Math.max(0, boundingClientRect.x - parentPosition.x);
    const newY = Math.max(0, boundingClientRect.y - parentPosition.y);

    this.noteService.updatePosition(note.id, { positionX: Math.round(newX), positionY: Math.round(newY) }).subscribe({
      error: () => this.errorMessage = 'Error al guardar la posición.'
    });
    
    event.source._dragRef.reset();
    
    note.positionX = Math.round(newX);
    note.positionY = Math.round(newY);
  }

  private getBoardOffset() {
    const boardEl = document.querySelector('.board-canvas');
    if (boardEl) {
      const rect = boardEl.getBoundingClientRect();
      return { x: rect.left, y: rect.top };
    }
    return { x: 0, y: 0 };
  }

  logout() {
    this.authService.logout();
  }
}
