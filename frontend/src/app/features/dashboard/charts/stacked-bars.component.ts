import { Component, ElementRef, computed, inject, input, output, signal } from '@angular/core';
import { ChartTooltipComponent } from './chart-tooltip.component';
import { ChartSegment, LegendItem, StackedRow, TooltipState, formatNumber, percentOf } from './chart.models';

/**
 * Barras horizontales apiladas (parte-del-todo por fila), todas con la misma escala.
 * Separación de 2px entre segmentos, extremo redondeado y total al final de la barra.
 */
@Component({
  selector: 'app-stacked-bars',
  imports: [ChartTooltipComponent],
  template: `
    <ul class="legend">
      @for (item of legend(); track item.key) {
        <li><span class="swatch" [style.background]="item.color"></span>{{ item.label }}</li>
      }
    </ul>

    <div class="rows">
      @for (row of visibleRows(); track row.id) {
        <div class="row" [class.selected]="row.id === selectedId()" [class.muted]="row.muted"
             role="button" tabindex="0"
             [attr.aria-label]="row.label + ', ' + rowTotal(row) + ' en total'"
             (click)="rowClick.emit(row.id)" (keydown.enter)="rowClick.emit(row.id)">
          <div class="row-label">
            <span class="name">{{ row.label }}
              @if (row.badge) { <span class="badge">{{ row.badge }}</span> }
            </span>
            @if (row.meta) { <span class="meta">{{ row.meta }}</span> }
          </div>
          <div class="track">
            <div class="bar" [style.width.%]="barWidth(row)">
              @for (segment of nonZero(row); track segment.key) {
                <span class="segment" [style.flex-grow]="segment.value" [style.background]="segment.color"
                      (pointermove)="showTooltip($event, row, segment)" (pointerleave)="tooltip.set(null)"></span>
              }
            </div>
            <span class="total">{{ format(rowTotal(row)) }}</span>
          </div>
        </div>
      } @empty {
        <p class="empty">{{ emptyText() }}</p>
      }
      <app-chart-tooltip [state]="tooltip()" />
    </div>

    @if (rows().length > limit()) {
      <button type="button" class="more" (click)="expanded.set(!expanded())">
        {{ expanded() ? 'Ver menos' : 'Ver todos (' + rows().length + ')' }}
      </button>
    }
  `,
  styles: [`
    :host { display: block; }
    .legend { list-style: none; display: flex; gap: 1rem; flex-wrap: wrap; margin: 0 0 0.75rem; padding: 0; font-size: 0.82rem; color: var(--ink-2); }
    .legend li { display: flex; align-items: center; gap: 0.35rem; }
    .swatch { width: 12px; height: 12px; border-radius: 3px; }
    .rows { position: relative; display: flex; flex-direction: column; gap: 0.15rem; }
    .row {
      display: grid; grid-template-columns: minmax(130px, 38%) 1fr; align-items: center; gap: 0.75rem;
      padding: 0.4rem 0.5rem; border-radius: 6px; cursor: pointer; outline: none;
    }
    .row:hover, .row:focus-visible { background: #f3f4f6; }
    .row.selected { background: #eff6ff; box-shadow: inset 3px 0 0 #2563eb; }
    .row.muted .name { color: var(--ink-3); }
    .row-label { display: flex; flex-direction: column; min-width: 0; }
    .name { font-size: 0.9rem; color: var(--ink-1); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .badge { font-size: 0.7rem; font-weight: 500; color: var(--ink-3); margin-left: 0.25rem; }
    .meta { font-size: 0.75rem; color: var(--ink-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .track { display: flex; align-items: center; gap: 0.5rem; min-width: 0; }
    .bar { display: flex; gap: 2px; height: 14px; min-width: 0; }
    .segment { min-width: 3px; height: 100%; }
    .segment:last-child { border-radius: 0 4px 4px 0; }
    .segment:hover { filter: brightness(1.12); }
    .total { font-size: 0.85rem; font-weight: 600; color: var(--ink-1); font-variant-numeric: tabular-nums; white-space: nowrap; }
    .empty { color: var(--ink-3); font-size: 0.9rem; margin: 0.5rem 0; }
    .more { margin-top: 0.5rem; background: none; border: none; color: #2563eb; cursor: pointer; font: inherit; font-size: 0.85rem; padding: 0.25rem 0.5rem; }
    .more:hover { text-decoration: underline; }
  `]
})
export class StackedBarsComponent {
  private host = inject(ElementRef<HTMLElement>);

  readonly rows = input.required<StackedRow[]>();
  readonly legend = input.required<LegendItem[]>();
  readonly selectedId = input<number | null>(null);
  readonly limit = input(8);
  readonly emptyText = input('Sin datos para los filtros seleccionados.');
  readonly rowClick = output<number>();

  protected expanded = signal(false);
  protected tooltip = signal<TooltipState | null>(null);

  protected visibleRows = computed(() => this.expanded() ? this.rows() : this.rows().slice(0, this.limit()));

  private maxTotal = computed(() => Math.max(0, ...this.rows().map(row => this.rowTotal(row))));

  protected format = formatNumber;

  protected rowTotal(row: StackedRow): number {
    return row.segments.reduce((sum, s) => sum + s.value, 0);
  }

  protected nonZero(row: StackedRow): ChartSegment[] {
    return row.segments.filter(s => s.value > 0);
  }

  protected barWidth(row: StackedRow): number {
    const max = this.maxTotal();
    return max === 0 ? 0 : (this.rowTotal(row) / max) * 88;
  }

  protected showTooltip(event: PointerEvent, row: StackedRow, segment: ChartSegment) {
    const rect = (this.host.nativeElement.querySelector('.rows') as HTMLElement).getBoundingClientRect();
    this.tooltip.set({
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
      title: row.label,
      rows: [{
        label: `${segment.label} · ${percentOf(segment.value, this.rowTotal(row))}`,
        value: formatNumber(segment.value),
        color: segment.color
      }]
    });
  }
}
