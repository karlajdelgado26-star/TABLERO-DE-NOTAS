import { Component, computed, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BOARD_TYPE_LABELS } from '../../../shared/models/board.model';
import { DashboardNoteRow } from '../../../shared/models/dashboard.model';
import { NOTE_STATUS_LABELS, NoteStatus } from '../../../shared/models/note.model';
import { formatDate, formatRelative } from '../../../shared/utils/date-format';

const PAGE_SIZE = 10;

/** Detalle de las notas filtradas: quién la creó, en qué tablero está y en qué estado va. */
@Component({
  selector: 'app-notes-table',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="toolbar">
      <div class="chips" role="group" aria-label="Filtrar por estado">
        <button type="button" [class.active]="status() === null" (click)="setStatus(null)">
          Todas <span>{{ rows().length }}</span>
        </button>
        @for (item of statuses; track item) {
          <button type="button" [class.active]="status() === item" (click)="setStatus(item)">
            <span class="status-dot" [class]="'status-dot ' + item.toLowerCase()"></span>
            {{ statusLabels[item] }} <span>{{ countByStatus()[item] }}</span>
          </button>
        }
      </div>
      <input type="search" class="search" placeholder="Buscar por título, tablero o autor"
             [ngModel]="search()" (ngModelChange)="search.set($event); page.set(0)">
    </div>

    <div class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>Nota</th>
            <th>Tablero</th>
            <th>Autor</th>
            <th>Estado</th>
            <th>Creada</th>
            <th>Completada</th>
            <th>Último cambio</th>
          </tr>
        </thead>
        <tbody>
          @for (row of pageRows(); track row.id) {
            <tr>
              <td class="title"><a [routerLink]="['/boards', row.boardId]">{{ row.title }}</a></td>
              <td>{{ row.boardName }} <span class="muted">· {{ typeLabels[row.boardType] }}</span></td>
              <td>
                @if (row.createdBy) {
                  {{ row.createdBy.name }}
                  @if (!row.createdBy.active) { <span class="muted">(inactivo)</span> }
                } @else { <span class="muted">Sin autor</span> }
              </td>
              <td class="nowrap"><span class="status-dot" [class]="'status-dot ' + row.status.toLowerCase()"></span> {{ statusLabels[row.status] }}</td>
              <td class="nowrap">{{ date(row.createdAt) }}</td>
              <td class="nowrap">{{ row.completedAt ? date(row.completedAt) : '—' }}</td>
              <td class="nowrap">{{ relative(row.updatedAt ?? row.createdAt) }}</td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">No hay notas para los filtros seleccionados.</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (totalPages() > 1) {
      <div class="pager">
        <button type="button" class="btn btn-secondary btn-small" [disabled]="page() === 0" (click)="page.set(page() - 1)">Anterior</button>
        <span>Página {{ page() + 1 }} de {{ totalPages() }} · {{ filtered().length }} notas</span>
        <button type="button" class="btn btn-secondary btn-small" [disabled]="page() + 1 >= totalPages()" (click)="page.set(page() + 1)">Siguiente</button>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
    .chips { display: flex; gap: 0.35rem; flex-wrap: wrap; }
    .chips button {
      display: inline-flex; align-items: center; gap: 0.35rem; border: 1px solid #e5e7eb; background: white;
      border-radius: 999px; padding: 0.25rem 0.7rem; font: inherit; font-size: 0.85rem; color: var(--ink-2); cursor: pointer;
    }
    .chips button span:last-child { color: var(--ink-3); font-variant-numeric: tabular-nums; }
    .chips button.active { border-color: #2563eb; background: #eff6ff; color: #1d4ed8; }
    .search { padding: 0.4rem 0.6rem; border: 1px solid #d1d5db; border-radius: 4px; font: inherit; min-width: 240px; }
    .table-wrapper { overflow-x: auto; }
    .title a { color: #1d4ed8; text-decoration: none; font-weight: 500; }
    .title a:hover { text-decoration: underline; }
    .nowrap { white-space: nowrap; }
    .muted { color: var(--ink-3); }
    .empty { text-align: center; color: var(--ink-3); padding: 1.25rem; }
    .pager { display: flex; justify-content: flex-end; align-items: center; gap: 0.75rem; margin-top: 0.75rem; font-size: 0.85rem; color: var(--ink-3); }
  `]
})
export class NotesTableComponent {
  readonly rows = input.required<DashboardNoteRow[]>();
  /** Estado seleccionado (se puede fijar desde la dona). */
  readonly status = model<NoteStatus | null>(null);

  protected readonly statuses: NoteStatus[] = ['PENDING', 'IN_PROGRESS', 'COMPLETED'];
  protected readonly statusLabels = NOTE_STATUS_LABELS;
  protected readonly typeLabels = BOARD_TYPE_LABELS;

  protected search = signal('');
  protected page = signal(0);

  protected countByStatus = computed(() => {
    const counts: Record<NoteStatus, number> = { PENDING: 0, IN_PROGRESS: 0, COMPLETED: 0 };
    for (const row of this.rows()) {
      counts[row.status]++;
    }
    return counts;
  });

  protected filtered = computed(() => {
    const status = this.status();
    const term = this.search().trim().toLowerCase();
    return this.rows().filter(row =>
      (status === null || row.status === status)
      && (!term
        || row.title.toLowerCase().includes(term)
        || row.boardName.toLowerCase().includes(term)
        || (row.createdBy?.name.toLowerCase().includes(term) ?? false))
    );
  });

  protected totalPages = computed(() => Math.ceil(this.filtered().length / PAGE_SIZE));

  protected pageRows = computed(() => {
    const maxPage = Math.max(0, this.totalPages() - 1);
    const page = Math.min(this.page(), maxPage);
    return this.filtered().slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  });

  setStatus(status: NoteStatus | null) {
    this.status.set(status);
    this.page.set(0);
  }

  protected date(value: string): string {
    return formatDate(value);
  }

  protected relative(value: string): string {
    return formatRelative(value);
  }
}
