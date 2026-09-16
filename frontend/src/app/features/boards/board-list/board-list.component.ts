import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AppHeaderComponent } from '../../../shared/components/app-header/app-header.component';
import {
  BOARD_STATUSES, BOARD_STATUS_LABELS, BOARD_TYPES, BOARD_TYPE_LABELS, Board, BoardStatus, BoardType
} from '../../../shared/models/board.model';
import { getErrorMessage } from '../../../shared/utils/http-error';
import { BoardService } from '../services/board.service';

@Component({
  selector: 'app-board-list',
  imports: [FormsModule, RouterLink, AppHeaderComponent],
  templateUrl: './board-list.component.html',
  styleUrl: './board-list.component.css'
})
export class BoardListComponent implements OnInit {
  private boardService = inject(BoardService);
  protected authService = inject(AuthService);

  protected boards = signal<Board[]>([]);
  protected loading = signal(true);
  protected saving = signal(false);
  protected errorMessage = signal('');

  /** null = formulario cerrado; 'new' = crear; Board = editar ese tablero */
  protected formTarget = signal<Board | 'new' | null>(null);
  protected formName = '';
  protected formDescription = '';
  protected formType: BoardType = 'PROJECT';
  protected formStatus: BoardStatus = 'ACTIVE';

  protected readonly boardTypes = BOARD_TYPES;
  protected readonly boardStatuses = BOARD_STATUSES;
  protected readonly typeLabels = BOARD_TYPE_LABELS;
  protected readonly statusLabels = BOARD_STATUS_LABELS;

  ngOnInit() {
    this.loadBoards();
  }

  loadBoards() {
    this.loading.set(true);
    this.boardService.getBoards().subscribe({
      next: boards => {
        this.boards.set(boards);
        this.loading.set(false);
      },
      error: err => {
        this.errorMessage.set(getErrorMessage(err, 'Error al cargar los tableros.'));
        this.loading.set(false);
      }
    });
  }

  openCreate() {
    this.formName = '';
    this.formDescription = '';
    this.formType = 'PROJECT';
    this.formStatus = 'ACTIVE';
    this.formTarget.set('new');
  }

  openEdit(board: Board) {
    this.formName = board.name;
    this.formDescription = board.description ?? '';
    this.formType = board.type;
    this.formStatus = board.status;
    this.formTarget.set(board);
  }

  closeForm() {
    this.formTarget.set(null);
  }

  saveBoard() {
    const target = this.formTarget();
    const name = this.formName.trim();
    if (!target || !name) {
      this.errorMessage.set('El nombre del tablero es obligatorio.');
      return;
    }

    const request = {
      name,
      description: this.formDescription.trim() || null,
      type: this.formType,
      status: this.formStatus
    };
    const request$ = target === 'new'
      ? this.boardService.createBoard(request)
      : this.boardService.updateBoard(target.id, request);

    this.saving.set(true);
    request$.subscribe({
      next: saved => {
        this.boards.update(boards => {
          const others = boards.filter(b => b.id !== saved.id);
          return [...others, saved].sort((a, b) => a.name.localeCompare(b.name));
        });
        this.saving.set(false);
        this.errorMessage.set('');
        this.closeForm();
      },
      error: err => {
        this.saving.set(false);
        this.errorMessage.set(getErrorMessage(err, 'Error al guardar el tablero.'));
      }
    });
  }

  deleteBoard(board: Board) {
    const notesText = board.noteCount === 1 ? '1 nota' : `${board.noteCount} notas`;
    if (!confirm(`¿Eliminar el tablero "${board.name}"? También se eliminarán sus ${notesText}.`)) {
      return;
    }
    this.boardService.deleteBoard(board.id).subscribe({
      next: () => this.boards.update(boards => boards.filter(b => b.id !== board.id)),
      error: err => this.errorMessage.set(getErrorMessage(err, 'Error al eliminar el tablero.'))
    });
  }

  isEditing(): boolean {
    const target = this.formTarget();
    return target !== null && target !== 'new';
  }
}
