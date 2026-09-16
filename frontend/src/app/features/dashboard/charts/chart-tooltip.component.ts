import { Component, input } from '@angular/core';
import { TooltipState } from './chart.models';

/** Tooltip compartido: el valor va primero y en negrita, la etiqueta después. */
@Component({
  selector: 'app-chart-tooltip',
  template: `
    @if (state(); as tip) {
      <div class="tooltip" role="status" [class]="'tooltip ' + (tip.placement ?? 'above')"
           [style.left.px]="tip.x" [style.top.px]="tip.y">
        <div class="tooltip-title">{{ tip.title }}</div>
        @for (row of tip.rows; track row.label) {
          <div class="tooltip-row">
            <span class="key" [style.background]="row.color"></span>
            <strong>{{ row.value }}</strong>
            <span class="label">{{ row.label }}</span>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    :host { position: absolute; inset: 0; pointer-events: none; z-index: 5; }
    .tooltip {
      position: absolute;
      transform: translate(-50%, calc(-100% - 10px));
      background: #111827;
      color: #f9fafb;
      border-radius: 6px;
      padding: 0.45rem 0.6rem;
      font-size: 0.78rem;
      line-height: 1.35;
      white-space: nowrap;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
    }
    .tooltip.right { transform: translate(12px, 0); }
    .tooltip.left { transform: translate(calc(-100% - 12px), 0); }
    .tooltip-title { color: #d1d5db; margin-bottom: 0.15rem; }
    .tooltip-row { display: flex; align-items: center; gap: 0.4rem; }
    .key { width: 12px; height: 2px; border-radius: 1px; }
    strong { font-size: 0.85rem; color: #ffffff; }
    .label { color: #d1d5db; }
  `]
})
export class ChartTooltipComponent {
  readonly state = input<TooltipState | null>(null);
}
