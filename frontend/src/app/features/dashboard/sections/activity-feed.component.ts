import { Component, DestroyRef, OnChanges, SimpleChanges, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  ACTIVITY_ACTIONS, ACTIVITY_ACTION_LABELS, Activity, ActivityAction
} from '../../../shared/models/activity.model';
import { BOARD_STATUS_LABELS, BoardStatus } from '../../../shared/models/board.model';
import { DashboardFilters } from '../../../shared/models/dashboard.model';
import { NOTE_STATUS_LABELS, NoteStatus } from '../../../shared/models/note.model';
import { formatDateTime, formatRelative } from '../../../shared/utils/date-format';
import { getErrorMessage } from '../../../shared/utils/http-error';
import { DashboardService } from '../services/dashboard.service';

const PAGE_SIZE = 15;

const VERBS: Record<ActivityAction, string> = {
  NOTE_CREATED: 'creó la nota',
  NOTE_UPDATED: 'editó la nota',
  NOTE_STATUS_CHANGED: 'cambió el estado de',
  NOTE_MOVED: 'movió la nota',
  NOTE_DELETED: 'eliminó la nota',
  BOARD_CREATED: 'creó el tablero',
  BOARD_UPDATED: 'editó el tablero',
  BOARD_STATUS_CHANGED: 'cambió el estado del tablero',
  BOARD_DELETED: 'eliminó el tablero',
};

/** Línea de tiempo de lo que hicieron los empleados, con los filtros del dashboard. */
@Component({
  selector: 'app-activity-feed',
  imports: [FormsModule],
  template: `
    <div class="toolbar">
      <label class="action-filter">
        <span>Acción</span>
        <select [ngModel]="action()" (ngModelChange)="changeAction($event)">
          <option [ngValue]="null">Todas</option>
          @for (item of actions; track item) {
            <option [ngValue]="item">{{ actionLabels[item] }}</option>
          }
        </select>
      </label>
      <span class="count">{{ total() }} {{ total() === 1 ? 'acción' : 'acciones' }}</span>
    </div>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    <ol class="feed" [class.refreshing]="loading()">
      @for (item of items(); track item.id) {
        <li class="entry">
          <span class="avatar" [class.inactive]="!item.user.active" aria-hidden="true">{{ initials(item.user.name) }}</span>
          <div class="body">
            <p class="sentence">
              <strong>{{ item.user.name }}</strong>
              @if (!item.user.active) { <span class="muted">(inactivo)</span> }
              {{ verb(item) }}
              <strong>“{{ isBoardAction(item) ? item.boardName : item.noteTitle }}”</strong>
              @if (item.fromValue && item.toValue && isStatusChange(item)) {
                de <span class="pill">@if (!isBoardAction(item)) {<span class="status-dot" [class]="'status-dot ' + item.fromValue.toLowerCase()"></span>}{{ statusLabel(item, item.fromValue) }}</span>
                a <span class="pill">@if (!isBoardAction(item)) {<span class="status-dot" [class]="'status-dot ' + item.toValue.toLowerCase()"></span>}{{ statusLabel(item, item.toValue) }}</span>
              }
              @if (item.noteOwner && item.noteOwner.id !== item.user.id) {
                <span class="muted">· nota de {{ item.noteOwner.name }}</span>
              }
            </p>
            <p class="meta">
              @if (!isBoardAction(item) && item.boardName) { <span>{{ item.boardName }}</span> · }
              @if (item.details) { <span>{{ item.details }}</span> · }
              <time [attr.title]="fullDate(item.createdAt)">{{ relative(item.createdAt) }}</time>
            </p>
          </div>
        </li>
      } @empty {
        @if (!loading()) {
          <li class="empty">No hay actividad para los filtros seleccionados.</li>
        }
      }
    </ol>

    @if (hasMore()) {
      <button type="button" class="more btn btn-secondary btn-small" [disabled]="loading()" (click)="loadMore()">
        {{ loading() ? 'Cargando...' : 'Cargar más' }}
      </button>
    }
  `,
  styles: [`
    :host { display: block; }
    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 0.5rem; }
    .action-filter { display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; color: var(--ink-2); }
    .action-filter select { padding: 0.3rem 0.4rem; border: 1px solid #d1d5db; border-radius: 4px; font: inherit; background: white; }
    .count { font-size: 0.82rem; color: var(--ink-3); }
    .error { color: #b91c1c; font-size: 0.9rem; }
    .feed { list-style: none; margin: 0; padding: 0; max-height: 460px; overflow-y: auto; transition: opacity 0.15s ease; }
    .feed.refreshing { opacity: 0.55; }
    .entry { display: flex; gap: 0.7rem; padding: 0.6rem 0.25rem; border-bottom: 1px solid #f3f4f6; }
    .avatar {
      width: 32px; height: 32px; border-radius: 999px; background: #e0e7ff; color: #3730a3;
      display: flex; align-items: center; justify-content: center; font-size: 0.75rem; font-weight: 700; flex-shrink: 0;
    }
    .avatar.inactive { background: #e5e7eb; color: #6b7280; }
    .body { min-width: 0; }
    .sentence { margin: 0; font-size: 0.9rem; color: var(--ink-2); line-height: 1.45; }
    .sentence strong { color: var(--ink-1); font-weight: 600; }
    .pill {
      display: inline-flex; align-items: center; gap: 0.3rem; padding: 0 0.4rem; border-radius: 999px;
      background: #f3f4f6; color: var(--ink-1); font-size: 0.8rem; white-space: nowrap;
    }
    .pill .status-dot { width: 8px; height: 8px; }
    .muted { color: var(--ink-3); }
    .meta { margin: 0.15rem 0 0; font-size: 0.78rem; color: var(--ink-3); }
    .empty { padding: 1rem 0; color: var(--ink-3); font-size: 0.9rem; }
    .more { margin-top: 0.75rem; }
  `]
})
export class ActivityFeedComponent implements OnChanges {
  private dashboardService = inject(DashboardService);
  private destroyRef = inject(DestroyRef);

  readonly filters = input.required<DashboardFilters>();
  /** Cambia cada vez que el dashboard se recarga para volver a pedir el historial. */
  readonly refreshKey = input(0);

  protected readonly actions = ACTIVITY_ACTIONS;
  protected readonly actionLabels = ACTIVITY_ACTION_LABELS;

  protected items = signal<Activity[]>([]);
  protected total = signal(0);
  protected hasMore = signal(false);
  protected loading = signal(false);
  protected error = signal('');
  protected action = signal<ActivityAction | null>(null);

  private page = 0;
  private request?: Subscription;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['filters'] || changes['refreshKey']) {
      this.load(true);
    }
  }

  changeAction(action: ActivityAction | null) {
    this.action.set(action);
    this.load(true);
  }

  loadMore() {
    this.load(false);
  }

  private load(reset: boolean) {
    this.request?.unsubscribe();
    this.page = reset ? 0 : this.page + 1;
    this.loading.set(true);
    this.error.set('');

    this.request = this.dashboardService.getActivity(this.filters(), this.action(), this.page, PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.items.set(reset ? result.content : [...this.items(), ...result.content]);
          this.total.set(result.totalElements);
          this.hasMore.set(result.page + 1 < result.totalPages);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(getErrorMessage(err, 'No se pudo cargar el historial.'));
          this.loading.set(false);
        }
      });
  }

  protected verb(item: Activity): string {
    return VERBS[item.action];
  }

  protected isBoardAction(item: Activity): boolean {
    return item.action.startsWith('BOARD_');
  }

  protected isStatusChange(item: Activity): boolean {
    return item.action === 'NOTE_STATUS_CHANGED' || item.action === 'BOARD_STATUS_CHANGED';
  }

  protected statusLabel(item: Activity, value: string): string {
    return this.isBoardAction(item)
      ? BOARD_STATUS_LABELS[value as BoardStatus] ?? value
      : NOTE_STATUS_LABELS[value as NoteStatus] ?? value;
  }

  protected initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0].toUpperCase()).join('');
  }

  protected relative(value: string): string {
    return formatRelative(value);
  }

  protected fullDate(value: string): string {
    return formatDateTime(value);
  }
}
