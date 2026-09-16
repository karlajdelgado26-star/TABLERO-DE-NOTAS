import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragEnd, DragDropModule } from '@angular/cdk/drag-drop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NoteService } from './services/note.service';
import { NoteCardComponent } from './components/note-card/note-card.component';
import { BoardService } from '../boards/services/board.service';
import { AuthService } from '../../core/services/auth.service';
import { AppHeaderComponent } from '../../shared/components/app-header/app-header.component';
import { BOARD_STATUS_LABELS, BOARD_TYPE_LABELS, Board } from '../../shared/models/board.model';
import { Note, NoteCreateRequest, NoteUpdateRequest } from '../../shared/models/note.model';
import { getErrorMessage } from '../../shared/utils/http-error';

@Component({
  selector: 'app-board',
  imports: [DragDropModule, RouterLink, NoteCardComponent, AppHeaderComponent],
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private noteService = inject(NoteService);
  private boardService = inject(BoardService);
  protected authService = inject(AuthService);

  protected notes = this.noteService.notes;
  protected board = signal<Board | null>(null);
  protected errorMessage = signal('');
  protected readonly typeLabels = BOARD_TYPE_LABELS;
  protected readonly statusLabels = BOARD_STATUS_LABELS;

  private boardId = 0;

  ngOnInit() {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const id = Number(params.get('id'));
        if (!Number.isInteger(id) || id <= 0) {
          this.router.navigate(['/boards']);
          return;
        }
        this.boardId = id;
        this.loadBoard();
      });
  }

  loadBoard() {
    this.board.set(null);
    this.noteService.clear();

    this.boardService.getBoard(this.boardId).subscribe({
      next: board => this.board.set(board),
      error: err => this.errorMessage.set(
        err.status === 404 ? 'El tablero no existe o fue eliminado.' : getErrorMessage(err, 'Error al cargar el tablero.')
      )
    });

    this.loadNotes();
  }

  loadNotes() {
    this.noteService.loadNotes(this.boardId).subscribe({
      error: err => {
        if (err.status !== 404) {
          this.errorMessage.set(getErrorMessage(err, 'Error al cargar las notas.'));
        }
      }
    });
  }

  createNote() {
    // Escalona las notas nuevas para que no queden una encima de otra
    const offset = (this.notes().length % 6) * 30;
    const newNote: NoteCreateRequest = {
      title: 'Nueva Nota',
      content: '',
      status: 'PENDING',
      positionX: 50 + offset,
      positionY: 50 + offset
    };

    this.noteService.createNote(this.boardId, newNote).subscribe({
      error: err => this.errorMessage.set(getErrorMessage(err, 'Error al crear la nota.'))
    });
  }

  onUpdateNote(event: { id: number, request: NoteUpdateRequest }) {
    this.noteService.updateNote(event.id, event.request).subscribe({
      error: err => this.errorMessage.set(getErrorMessage(err, 'Error al actualizar la nota.'))
    });
  }

  onDeleteNote(id: number) {
    this.noteService.deleteNote(id).subscribe({
      error: err => this.errorMessage.set(getErrorMessage(err, 'Error al eliminar la nota.'))
    });
  }

  onDragEnded(event: CdkDragEnd, note: Note) {
    const element = event.source.getRootElement();
    const boundingClientRect = element.getBoundingClientRect();
    const parentPosition = this.getBoardOffset();

    const newX = Math.round(Math.max(0, boundingClientRect.x - parentPosition.x));
    const newY = Math.round(Math.max(0, boundingClientRect.y - parentPosition.y));

    event.source.reset();
    note.positionX = newX;
    note.positionY = newY;

    this.noteService.updatePosition(note.id, { positionX: newX, positionY: newY }).subscribe({
      error: err => {
        this.errorMessage.set(getErrorMessage(err, 'Error al guardar la posición.'));
        this.loadNotes();
      }
    });
  }

  private getBoardOffset() {
    const boardEl = document.querySelector('.board-canvas');
    if (boardEl) {
      const rect = boardEl.getBoundingClientRect();
      return { x: rect.left, y: rect.top };
    }
    return { x: 0, y: 0 };
  }
}
