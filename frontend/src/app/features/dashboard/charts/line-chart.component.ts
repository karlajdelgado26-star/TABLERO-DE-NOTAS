import {
  AfterViewInit, Component, DestroyRef, ElementRef, computed, inject, input, signal, viewChild
} from '@angular/core';
import { ChartTooltipComponent } from './chart-tooltip.component';
import { LineSeries, TooltipState, formatNumber, niceMax } from './chart.models';

const HEIGHT = 230;
const PAD = { top: 14, right: 16, bottom: 28, left: 36 };

/**
 * Líneas de 2px sobre un solo eje, con crosshair que se ajusta al punto más cercano
 * y tooltip con todas las series. Se navega también con las flechas del teclado.
 */
@Component({
  selector: 'app-line-chart',
  imports: [ChartTooltipComponent],
  template: `
    <ul class="legend">
      @for (serie of series(); track serie.key) {
        <li><span class="line-key" [style.background]="serie.color"></span>{{ serie.label }}
          <strong>{{ format(sum(serie)) }}</strong></li>
      }
    </ul>
    <div class="chart" #container>
      <svg [attr.width]="width()" [attr.height]="height" tabindex="0" role="img"
           [attr.aria-label]="ariaLabel()"
           (pointermove)="onPointer($event)" (pointerleave)="clear()"
           (keydown)="onKey($event)" (blur)="clear()">
        <!-- rejilla y eje Y -->
        @for (tick of yTicks(); track tick.value) {
          <line class="grid" [class.baseline]="tick.value === 0"
                [attr.x1]="pad.left" [attr.x2]="width() - pad.right" [attr.y1]="tick.y" [attr.y2]="tick.y" />
          <text class="axis" [attr.x]="pad.left - 8" [attr.y]="tick.y + 4" text-anchor="end">{{ format(tick.value) }}</text>
        }
        <!-- eje X -->
        @for (tick of xTicks(); track tick.index) {
          <text class="axis" [attr.x]="tick.x" [attr.y]="height - 8" [attr.text-anchor]="tick.anchor">{{ tick.label }}</text>
        }
        <!-- series -->
        @for (path of paths(); track path.key) {
          <path class="line" [attr.d]="path.d" [style.stroke]="path.color" />
        }
        <!-- crosshair -->
        @if (activeIndex() !== null) {
          <line class="crosshair" [attr.x1]="xAt(activeIndex()!)" [attr.x2]="xAt(activeIndex()!)"
                [attr.y1]="pad.top" [attr.y2]="height - pad.bottom" />
          @for (serie of series(); track serie.key) {
            <circle class="dot" [attr.cx]="xAt(activeIndex()!)" [attr.cy]="yAt(serie.values[activeIndex()!])"
                    r="5" [style.fill]="serie.color" />
          }
        }
      </svg>
      <app-chart-tooltip [state]="tooltip()" />
    </div>
  `,
  styles: [`
    :host { display: block; }
    .legend { list-style: none; display: flex; gap: 1.25rem; flex-wrap: wrap; margin: 0 0 0.5rem; padding: 0; font-size: 0.85rem; color: var(--ink-2); }
    .legend li { display: flex; align-items: center; gap: 0.4rem; }
    .legend strong { color: var(--ink-1); }
    .line-key { width: 16px; height: 2px; border-radius: 1px; }
    .chart { position: relative; width: 100%; }
    svg { display: block; outline: none; }
    svg:focus-visible { box-shadow: 0 0 0 2px #93c5fd; border-radius: 4px; }
    .grid { stroke: var(--chart-grid); stroke-width: 1; }
    .grid.baseline { stroke: var(--chart-baseline); }
    .axis { fill: var(--ink-3); font-size: 11px; font-variant-numeric: tabular-nums; }
    .line { fill: none; stroke-width: 2; stroke-linejoin: round; stroke-linecap: round; }
    .crosshair { stroke: var(--ink-3); stroke-width: 1; }
    .dot { stroke: var(--chart-surface); stroke-width: 2; }
  `]
})
export class LineChartComponent implements AfterViewInit {
  private destroyRef = inject(DestroyRef);
  private container = viewChild.required<ElementRef<HTMLDivElement>>('container');

  readonly labels = input.required<string[]>();
  /** Texto completo para el tooltip de cada punto (ej. "Semana del 14 sept"). */
  readonly tooltipLabels = input<string[]>([]);
  readonly series = input.required<LineSeries[]>();

  protected readonly height = HEIGHT;
  protected readonly pad = PAD;
  protected width = signal(600);
  protected activeIndex = signal<number | null>(null);

  private scale = computed(() => niceMax(Math.max(0, ...this.series().flatMap(s => s.values))));

  protected yTicks = computed(() => {
    const { max, step } = this.scale();
    const ticks = [];
    for (let value = 0; value <= max; value += step) {
      ticks.push({ value, y: this.yAt(value) });
    }
    return ticks;
  });

  protected xTicks = computed(() => {
    const labels = this.labels();
    const count = labels.length;
    if (count === 0) {
      return [];
    }
    const maxTicks = Math.max(2, Math.floor((this.width() - PAD.left - PAD.right) / 90));
    const every = Math.max(1, Math.ceil((count - 1) / (maxTicks - 1)));
    const ticks: { index: number; x: number; label: string; anchor: string }[] = [];
    for (let i = 0; i < count; i += every) {
      ticks.push({ index: i, x: this.xAt(i), label: labels[i], anchor: i === 0 ? 'start' : 'middle' });
    }
    const last = count - 1;
    if (ticks[ticks.length - 1].index !== last) {
      // Si la última etiqueta quedaría pegada a la anterior, se quita la anterior
      if (ticks.length > 1 && this.xAt(last) - ticks[ticks.length - 1].x < 70) {
        ticks.pop();
      }
      ticks.push({ index: last, x: this.xAt(last), label: labels[last], anchor: 'end' });
    } else if (count > 1) {
      ticks[ticks.length - 1].anchor = 'end';
    }
    return ticks;
  });

  protected paths = computed(() => this.series().map(serie => ({
    key: serie.key,
    color: serie.color,
    d: serie.values.map((value, i) => `${i === 0 ? 'M' : 'L'}${this.xAt(i).toFixed(1)},${this.yAt(value).toFixed(1)}`).join(' ')
  })));

  protected tooltip = computed<TooltipState | null>(() => {
    const index = this.activeIndex();
    if (index === null) {
      return null;
    }
    const titles = this.tooltipLabels();
    const x = this.xAt(index);
    return {
      x,
      y: PAD.top + 4,
      placement: x > this.width() * 0.6 ? 'left' : 'right',
      title: titles[index] ?? this.labels()[index],
      rows: this.series().map(serie => ({
        label: serie.label,
        value: formatNumber(serie.values[index] ?? 0),
        color: serie.color
      }))
    };
  });

  protected ariaLabel = computed(() =>
    this.series().map(s => `${s.label}: ${this.sum(s)} en total`).join('. ')
  );

  protected format = formatNumber;

  ngAfterViewInit() {
    const element = this.container().nativeElement;
    this.width.set(Math.max(280, element.clientWidth));
    const observer = new ResizeObserver(entries => {
      const newWidth = Math.max(280, Math.floor(entries[0].contentRect.width));
      if (newWidth !== this.width()) {
        this.width.set(newWidth);
      }
    });
    observer.observe(element);
    this.destroyRef.onDestroy(() => observer.disconnect());
  }

  protected sum(serie: LineSeries): number {
    return serie.values.reduce((total, value) => total + value, 0);
  }

  protected xAt(index: number): number {
    const count = this.labels().length;
    const plotWidth = this.width() - PAD.left - PAD.right;
    return count <= 1 ? PAD.left + plotWidth / 2 : PAD.left + (index / (count - 1)) * plotWidth;
  }

  protected yAt(value: number): number {
    const plotHeight = HEIGHT - PAD.top - PAD.bottom;
    return PAD.top + plotHeight - (value / this.scale().max) * plotHeight;
  }

  protected onPointer(event: PointerEvent) {
    const svg = event.currentTarget as SVGSVGElement;
    const x = event.clientX - svg.getBoundingClientRect().left;
    const count = this.labels().length;
    if (count === 0) {
      return;
    }
    const plotWidth = this.width() - PAD.left - PAD.right;
    const ratio = (x - PAD.left) / plotWidth;
    this.activeIndex.set(Math.min(count - 1, Math.max(0, Math.round(ratio * (count - 1)))));
  }

  protected onKey(event: KeyboardEvent) {
    const count = this.labels().length;
    if (count === 0) {
      return;
    }
    const current = this.activeIndex();
    if (event.key === 'ArrowRight') {
      this.activeIndex.set(current === null ? 0 : Math.min(count - 1, current + 1));
      event.preventDefault();
    } else if (event.key === 'ArrowLeft') {
      this.activeIndex.set(current === null ? count - 1 : Math.max(0, current - 1));
      event.preventDefault();
    } else if (event.key === 'Escape') {
      this.clear();
    }
  }

  protected clear() {
    this.activeIndex.set(null);
  }
}
