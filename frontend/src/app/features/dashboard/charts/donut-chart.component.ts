import { Component, ElementRef, computed, inject, input, output, signal } from '@angular/core';
import { ChartTooltipComponent } from './chart-tooltip.component';
import { ChartSegment, TooltipState, formatNumber, percentOf } from './chart.models';

interface Arc extends ChartSegment {
  dash: string;
  offset: number;
}

const SIZE = 180;
const RADIUS = 70;
const STROKE = 22;
const GAP = 2;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/** Dona de parte-del-todo (máximo ~6 segmentos) con leyenda, conteos y tooltip. */
@Component({
  selector: 'app-donut-chart',
  imports: [ChartTooltipComponent],
  template: `
    <div class="donut-layout">
      <div class="donut-wrap">
        <svg [attr.viewBox]="'0 0 ' + size + ' ' + size" role="img" [attr.aria-label]="ariaLabel()">
          <circle [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="radius" class="track" [attr.stroke-width]="stroke" />
          @for (arc of arcs(); track arc.key) {
            <circle
              class="arc"
              [class.dimmed]="hovered() !== null && hovered() !== arc.key"
              [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="radius"
              [attr.stroke-width]="stroke"
              [style.stroke]="arc.color"
              [attr.stroke-dasharray]="arc.dash"
              [attr.stroke-dashoffset]="arc.offset"
              tabindex="0"
              [attr.aria-label]="arc.label + ': ' + arc.value"
              (pointermove)="showTooltip($event, arc)"
              (pointerleave)="hideTooltip()"
              (focus)="focusArc(arc)"
              (blur)="hideTooltip()"
              (click)="segmentClick.emit(arc.key)"
              (keydown.enter)="segmentClick.emit(arc.key)" />
          }
        </svg>
        <div class="center">
          <span class="center-value">{{ format(total()) }}</span>
          <span class="center-label">{{ centerLabel() }}</span>
        </div>
        <app-chart-tooltip [state]="tooltip()" />
      </div>

      <ul class="legend">
        @for (segment of segments(); track segment.key) {
          <li>
            <button type="button" (click)="segmentClick.emit(segment.key)"
                    (mouseenter)="hovered.set(segment.key)" (mouseleave)="hovered.set(null)">
              <span class="swatch" [style.background]="segment.color"></span>
              <span class="legend-label">{{ segment.label }}</span>
              <strong>{{ format(segment.value) }}</strong>
              <span class="pct">{{ percent(segment.value) }}</span>
            </button>
          </li>
        }
      </ul>
    </div>
  `,
  styles: [`
    .donut-layout { display: flex; align-items: center; justify-content: center; gap: 1rem 1.25rem; flex-wrap: wrap; }
    .donut-wrap { position: relative; width: 150px; height: 150px; flex-shrink: 0; }
    svg { width: 100%; height: 100%; transform: rotate(-90deg); overflow: visible; }
    .track { fill: none; stroke: var(--chart-grid); }
    .arc { fill: none; cursor: pointer; transition: opacity 0.15s ease; outline: none; }
    .arc:focus-visible { stroke-width: 26px; }
    .arc.dimmed { opacity: 0.35; }
    .center {
      position: absolute; inset: 0; display: flex; flex-direction: column;
      align-items: center; justify-content: center; pointer-events: none;
    }
    .center-value { font-size: 1.7rem; font-weight: 700; color: var(--ink-1); line-height: 1.1; }
    .center-label { font-size: 0.8rem; color: var(--ink-3); }
    .legend { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.25rem; min-width: 200px; flex: 1; }
    .legend button {
      width: 100%; display: grid; grid-template-columns: 12px 1fr auto 3rem; align-items: center; gap: 0.5rem;
      background: none; border: none; padding: 0.3rem 0.4rem; border-radius: 4px; cursor: pointer;
      font: inherit; color: var(--ink-2); text-align: left;
    }
    .legend button:hover { background: #f3f4f6; }
    .swatch { width: 12px; height: 12px; border-radius: 3px; }
    .legend-label { white-space: nowrap; }
    strong { color: var(--ink-1); font-variant-numeric: tabular-nums; }
    .pct { color: var(--ink-3); font-size: 0.85rem; text-align: right; font-variant-numeric: tabular-nums; }
  `]
})
export class DonutChartComponent {
  private host = inject(ElementRef<HTMLElement>);

  readonly segments = input.required<ChartSegment[]>();
  readonly centerLabel = input('');
  readonly segmentClick = output<string>();

  protected readonly size = SIZE;
  protected readonly radius = RADIUS;
  protected readonly stroke = STROKE;
  protected hovered = signal<string | null>(null);
  protected tooltip = signal<TooltipState | null>(null);

  protected total = computed(() => this.segments().reduce((sum, s) => sum + s.value, 0));

  protected arcs = computed<Arc[]>(() => {
    const total = this.total();
    const visible = this.segments().filter(s => s.value > 0);
    if (total === 0) {
      return [];
    }
    const gap = visible.length > 1 ? GAP : 0;
    let accumulated = 0;
    return visible.map(segment => {
      const length = (segment.value / total) * CIRCUMFERENCE;
      const dashLength = Math.max(length - gap, 0.5);
      const arc: Arc = {
        ...segment,
        dash: `${dashLength} ${CIRCUMFERENCE - dashLength}`,
        offset: -accumulated
      };
      accumulated += length;
      return arc;
    });
  });

  protected ariaLabel = computed(() =>
    this.segments().map(s => `${s.label}: ${s.value}`).join(', ')
  );

  protected format = formatNumber;

  protected percent(value: number): string {
    return percentOf(value, this.total());
  }

  protected showTooltip(event: PointerEvent, arc: Arc) {
    const rect = (this.host.nativeElement.querySelector('.donut-wrap') as HTMLElement).getBoundingClientRect();
    this.hovered.set(arc.key);
    this.tooltip.set(this.buildTooltip(arc, event.clientX - rect.left, event.clientY - rect.top));
  }

  protected focusArc(arc: Arc) {
    this.hovered.set(arc.key);
    this.tooltip.set(this.buildTooltip(arc, 85, 20));
  }

  protected hideTooltip() {
    this.hovered.set(null);
    this.tooltip.set(null);
  }

  private buildTooltip(arc: Arc, x: number, y: number): TooltipState {
    return {
      x,
      y,
      title: arc.label,
      rows: [{ label: `${this.percent(arc.value)} del total`, value: formatNumber(arc.value), color: arc.color }]
    };
  }
}
