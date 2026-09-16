import { Component, DestroyRef, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subject, switchMap, tap, catchError, of } from 'rxjs';
import { AppHeaderComponent } from '../../shared/components/app-header/app-header.component';
import { ACTIVITY_ACTION_LABELS, ActivityAction } from '../../shared/models/activity.model';
import {
  BOARD_STATUSES, BOARD_STATUS_LABELS, BOARD_TYPES, BOARD_TYPE_LABELS, Board, BoardStatus, BoardType
} from '../../shared/models/board.model';
import { Dashboard, DashboardFilters, EMPTY_DASHBOARD_FILTERS } from '../../shared/models/dashboard.model';
import { NOTE_STATUS_LABELS, NoteStatus } from '../../shared/models/note.model';
import { ROLE_LABELS } from '../../shared/models/user.model';
import { formatDateTime, formatRelative, formatShortDay, parseLocalDate, toIsoDate } from '../../shared/utils/date-format';
import { getErrorMessage } from '../../shared/utils/http-error';
import { BoardService } from '../boards/services/board.service';
import { BarListComponent } from './charts/bar-list.component';
import {
  BarItem, ChartSegment, LegendItem, LineSeries, StackedRow, formatNumber, formatPercent, percentOf
} from './charts/chart.models';
import { DonutChartComponent } from './charts/donut-chart.component';
import { LineChartComponent } from './charts/line-chart.component';
import { StackedBarsComponent } from './charts/stacked-bars.component';
import { ActivityFeedComponent } from './sections/activity-feed.component';
import { NotesTableComponent } from './sections/notes-table.component';
import { DashboardService } from './services/dashboard.service';

type PeriodPreset = 'ALL' | 'LAST_7' | 'LAST_30' | 'LAST_90' | 'THIS_MONTH' | 'CUSTOM';

const STATUS_COLORS: Record<NoteStatus, string> = {
  PENDING: 'var(--status-pending)',
  IN_PROGRESS: 'var(--status-in-progress)',
  COMPLETED: 'var(--status-completed)',
};

const STATUS_ORDER: NoteStatus[] = ['PENDING', 'IN_PROGRESS', 'COMPLETED'];

interface Option {
  id: number;
  name: string;
  active: boolean;
}

@Component({
  selector: 'app-dashboard',
  imports: [
    FormsModule, AppHeaderComponent, DonutChartComponent, BarListComponent, StackedBarsComponent,
    LineChartComponent, ActivityFeedComponent, NotesTableComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private boardService = inject(BoardService);
  private destroyRef = inject(DestroyRef);
  private notesSection = viewChild<ElementRef<HTMLElement>>('notesSection');

  protected readonly boardTypes = BOARD_TYPES;
  protected readonly boardStatuses = BOARD_STATUSES;
  protected readonly typeLabels = BOARD_TYPE_LABELS;
  protected readonly boardStatusLabels = BOARD_STATUS_LABELS;
  protected readonly noteStatusLabels = NOTE_STATUS_LABELS;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly statusOrder = STATUS_ORDER;
  protected readonly statusColors = STATUS_COLORS;

  // ---- Filtros
  protected filters = signal<DashboardFilters>({ ...EMPTY_DASHBOARD_FILTERS });
  protected period = signal<PeriodPreset>('ALL');
  protected customFrom = '';
  protected customTo = '';

  // ---- Datos
  protected data = signal<Dashboard | null>(null);
  protected loading = signal(true);
  protected errorMessage = signal('');
  protected refreshKey = signal(0);
  protected boardOptions = signal<Board[]>([]);
  protected userOptions = signal<Option[]>([]);
  protected noteStatusFilter = signal<NoteStatus | null>(null);
  /** Tarjetas que están mostrando la tabla en vez de la gráfica */
  protected tableView = signal<Record<string, boolean>>({});

  private reload$ = new Subject<DashboardFilters>();

  ngOnInit() {
    this.reload$
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set('');
        }),
        switchMap(filters => this.dashboardService.getDashboard(filters).pipe(
          catchError(err => {
            this.errorMessage.set(getErrorMessage(err, 'No se pudo cargar el dashboard.'));
            return of(null);
          })
        )),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(dashboard => {
        this.loading.set(false);
        if (!dashboard) {
          return;
        }
        this.data.set(dashboard);
        this.refreshKey.update(key => key + 1);
        this.rememberUsers(dashboard);
      });

    this.boardService.getBoards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: boards => this.boardOptions.set(boards), error: () => undefined });

    this.reload();
  }

  reload() {
    this.reload$.next(this.filters());
  }

  // ------------------------------------------------------------------ filtros

  protected setFilter<K extends keyof DashboardFilters>(key: K, value: DashboardFilters[K]) {
    this.filters.update(current => ({ ...current, [key]: value }));
    this.reload();
  }

  /** Clic en una barra o fila: aplica el filtro, o lo quita si ya estaba aplicado. */
  protected toggleFilter<K extends 'boardId' | 'userId' | 'boardType' | 'boardStatus'>(key: K, value: DashboardFilters[K]) {
    this.setFilter(key, this.filters()[key] === value ? null : value);
  }

  protected changePeriod(preset: PeriodPreset) {
    this.period.set(preset);
    if (preset === 'CUSTOM') {
      const current = this.filters();
      this.customFrom = current.from ?? toIsoDate(this.daysAgo(29));
      this.customTo = current.to ?? toIsoDate(new Date());
      this.applyCustomRange();
      return;
    }
    const today = new Date();
    const ranges: Record<Exclude<PeriodPreset, 'CUSTOM'>, [string | null, string | null]> = {
      ALL: [null, null],
      LAST_7: [toIsoDate(this.daysAgo(6)), toIsoDate(today)],
      LAST_30: [toIsoDate(this.daysAgo(29)), toIsoDate(today)],
      LAST_90: [toIsoDate(this.daysAgo(89)), toIsoDate(today)],
      THIS_MONTH: [toIsoDate(new Date(today.getFullYear(), today.getMonth(), 1)), toIsoDate(today)],
    };
    const [from, to] = ranges[preset];
    this.filters.update(current => ({ ...current, from, to }));
    this.reload();
  }

  protected applyCustomRange() {
    this.filters.update(current => ({ ...current, from: this.customFrom || null, to: this.customTo || null }));
    this.reload();
  }

  protected clearFilters() {
    this.period.set('ALL');
    this.customFrom = '';
    this.customTo = '';
    this.noteStatusFilter.set(null);
    this.filters.set({ ...EMPTY_DASHBOARD_FILTERS });
    this.reload();
  }

  protected hasFilters = computed(() => Object.values(this.filters()).some(value => value !== null));

  protected activeChips = computed(() => {
    const f = this.filters();
    const chips: { key: keyof DashboardFilters | 'period'; label: string }[] = [];
    if (f.from || f.to) {
      chips.push({ key: 'period', label: `Periodo: ${this.rangeLabel()}` });
    }
    if (f.boardId !== null) {
      const board = this.boardOptions().find(b => b.id === f.boardId);
      chips.push({ key: 'boardId', label: `Tablero: ${board?.name ?? '#' + f.boardId}` });
    }
    if (f.boardType) {
      chips.push({ key: 'boardType', label: `Tipo: ${BOARD_TYPE_LABELS[f.boardType]}` });
    }
    if (f.boardStatus) {
      chips.push({ key: 'boardStatus', label: `Estado del tablero: ${BOARD_STATUS_LABELS[f.boardStatus]}` });
    }
    if (f.userId !== null) {
      const user = this.userOptions().find(u => u.id === f.userId);
      chips.push({ key: 'userId', label: `Empleado: ${user?.name ?? '#' + f.userId}` });
    }
    return chips;
  });

  protected removeChip(key: keyof DashboardFilters | 'period') {
    if (key === 'period') {
      this.changePeriod('ALL');
      return;
    }
    this.setFilter(key, null);
  }

  protected rangeLabel = computed(() => {
    const { from, to } = this.filters();
    if (!from && !to) {
      return 'Todo el tiempo';
    }
    const format = (value: string | null) => value ? formatShortDay(value) + ' ' + parseLocalDate(value).getFullYear() : '…';
    return `${format(from)} – ${format(to)}`;
  });

  // ------------------------------------------------------------------ vistas de gráficas

  protected statusSegments = computed<ChartSegment[]>(() => {
    const dashboard = this.data();
    return STATUS_ORDER.map(status => ({
      key: status,
      label: NOTE_STATUS_LABELS[status],
      value: dashboard?.notesByStatus.find(item => item.key === status)?.count ?? 0,
      color: STATUS_COLORS[status]
    }));
  });

  protected statusLegend: LegendItem[] = STATUS_ORDER.map(status => ({
    key: status, label: NOTE_STATUS_LABELS[status], color: STATUS_COLORS[status]
  }));

  protected typeItems = computed<BarItem[]>(() =>
    (this.data()?.boardsByType ?? []).map(item => ({
      key: item.key, label: BOARD_TYPE_LABELS[item.key as BoardType] ?? item.key, value: item.count
    }))
  );

  protected boardStatusItems = computed<BarItem[]>(() =>
    (this.data()?.boardsByStatus ?? []).map(item => ({
      key: item.key, label: BOARD_STATUS_LABELS[item.key as BoardStatus] ?? item.key, value: item.count
    }))
  );

  protected activityItems = computed<BarItem[]>(() =>
    (this.data()?.activityByAction ?? [])
      .filter(item => item.count > 0)
      .sort((a, b) => b.count - a.count)
      .map(item => ({ key: item.key, label: ACTIVITY_ACTION_LABELS[item.key as ActivityAction] ?? item.key, value: item.count }))
  );

  protected timelineLabels = computed(() =>
    (this.data()?.timeline.points ?? []).map(point => formatShortDay(point.date))
  );

  protected timelineTooltipLabels = computed(() => {
    const timeline = this.data()?.timeline;
    if (!timeline) {
      return [];
    }
    return timeline.points.map(point => timeline.granularity === 'WEEK'
      ? `Semana del ${formatShortDay(point.date)}`
      : formatShortDay(point.date) + ' ' + parseLocalDate(point.date).getFullYear());
  });

  protected timelineSeries = computed<LineSeries[]>(() => {
    const points = this.data()?.timeline.points ?? [];
    return [
      { key: 'created', label: 'Notas creadas', color: 'var(--series-context)', values: points.map(p => p.created) },
      { key: 'completed', label: 'Notas completadas', color: 'var(--status-completed)', values: points.map(p => p.completed) }
    ];
  });

  protected timelineSubtitle = computed(() => {
    const timeline = this.data()?.timeline;
    if (!timeline) {
      return '';
    }
    const unit = timeline.granularity === 'WEEK' ? 'por semana' : 'por día';
    const range = `${formatShortDay(timeline.from)} ${parseLocalDate(timeline.from).getFullYear()} – ${formatShortDay(timeline.to)} ${parseLocalDate(timeline.to).getFullYear()}`;
    return `${unit} · ${range}${this.filters().from || this.filters().to ? '' : ' (últimos 30 días)'}`;
  });

  protected userRows = computed<StackedRow[]>(() =>
    (this.data()?.users ?? []).map(user => ({
      id: user.id,
      label: user.name,
      badge: user.active ? undefined : 'inactivo',
      muted: !user.active,
      meta: `${ROLE_LABELS[user.role]} · ${formatNumber(user.activities)} ${user.activities === 1 ? 'acción' : 'acciones'} · ${formatRelative(user.lastActivityAt)}`,
      segments: this.segmentsFor(user.notes)
    }))
  );

  protected boardRows = computed<StackedRow[]>(() =>
    (this.data()?.boards ?? []).map(board => ({
      id: board.id,
      label: board.name,
      muted: board.status === 'FINISHED',
      meta: `${BOARD_TYPE_LABELS[board.type]} · ${BOARD_STATUS_LABELS[board.status]} · ${formatPercent(board.notes.completionRate)} completado`,
      segments: this.segmentsFor(board.notes)
    }))
  );

  private segmentsFor(counts: { pending: number; inProgress: number; completed: number }): ChartSegment[] {
    return [
      { key: 'PENDING', label: NOTE_STATUS_LABELS.PENDING, value: counts.pending, color: STATUS_COLORS.PENDING },
      { key: 'IN_PROGRESS', label: NOTE_STATUS_LABELS.IN_PROGRESS, value: counts.inProgress, color: STATUS_COLORS.IN_PROGRESS },
      { key: 'COMPLETED', label: NOTE_STATUS_LABELS.COMPLETED, value: counts.completed, color: STATUS_COLORS.COMPLETED }
    ];
  }

  // ------------------------------------------------------------------ interacción

  protected isTable(card: string): boolean {
    return this.tableView()[card] ?? false;
  }

  protected toggleTable(card: string) {
    this.tableView.update(state => ({ ...state, [card]: !state[card] }));
  }

  protected onStatusClick(key: string) {
    this.noteStatusFilter.set(this.noteStatusFilter() === key ? null : key as NoteStatus);
    this.notesSection()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  protected onUserClick(id: number) {
    this.toggleFilter('userId', id);
  }

  protected onBoardClick(id: number) {
    this.toggleFilter('boardId', id);
  }

  protected onTypeClick(key: string) {
    this.toggleFilter('boardType', key as BoardType);
  }

  protected onBoardStatusClick(key: string) {
    this.toggleFilter('boardStatus', key as BoardStatus);
  }

  // ------------------------------------------------------------------ formato

  protected format = formatNumber;
  protected formatPct = formatPercent;

  protected percent(value: number, total: number): string {
    return percentOf(value, total);
  }

  protected relative(value: string | null): string {
    return formatRelative(value);
  }

  protected dateTime(value: string | null): string {
    return formatDateTime(value);
  }

  protected shortDay(value: string): string {
    return formatShortDay(value);
  }

  private daysAgo(days: number): Date {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return date;
  }

  /** Guarda la lista de empleados de la última respuesta sin filtro de empleado (para el selector). */
  private rememberUsers(dashboard: Dashboard) {
    if (this.filters().userId !== null) {
      return;
    }
    const known = new Map(this.userOptions().map(option => [option.id, option]));
    for (const user of dashboard.users) {
      known.set(user.id, { id: user.id, name: user.name, active: user.active });
    }
    this.userOptions.set([...known.values()].sort((a, b) => a.name.localeCompare(b.name)));
  }
}
