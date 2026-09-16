import { Component, computed, input, output } from '@angular/core';
import { BarItem, formatNumber } from './chart.models';

/** Barras horizontales de una sola serie (conteos), con el valor al final de cada barra. */
@Component({
  selector: 'app-bar-list',
  template: `
    <div class="rows">
      @for (item of items(); track item.key) {
        <button type="button" class="row" [class.selected]="item.key === selectedKey()"
                [style.grid-template-columns]="labelWidth() + ' 1fr'"
                [attr.aria-label]="item.label + ': ' + item.value"
                (click)="itemClick.emit(item.key)">
          <span class="label">{{ item.label }}</span>
          <span class="track">
            <span class="bar" [style.width.%]="width(item.value)"></span>
            <span class="value">{{ format(item.value) }}</span>
          </span>
        </button>
      }
    </div>
  `,
  styles: [`
    .rows { display: flex; flex-direction: column; gap: 0.15rem; }
    .row {
      display: grid; grid-template-columns: 6.5rem 1fr; align-items: center; gap: 0.75rem;
      width: 100%; background: none; border: none; border-radius: 6px; padding: 0.4rem 0.5rem;
      font: inherit; text-align: left; cursor: pointer;
    }
    .row:hover { background: #f3f4f6; }
    .row.selected { background: #eff6ff; box-shadow: inset 3px 0 0 #2563eb; }
    .label { font-size: 0.88rem; color: var(--ink-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .track { display: flex; align-items: center; gap: 0.5rem; }
    .bar { height: 14px; min-width: 2px; background: var(--series-neutral); border-radius: 0 4px 4px 0; }
    .value { font-size: 0.85rem; font-weight: 600; color: var(--ink-1); font-variant-numeric: tabular-nums; }
  `]
})
export class BarListComponent {
  readonly items = input.required<BarItem[]>();
  readonly selectedKey = input<string | null>(null);
  readonly labelWidth = input('6.5rem');
  readonly itemClick = output<string>();

  private max = computed(() => Math.max(0, ...this.items().map(i => i.value)));

  protected format = formatNumber;

  protected width(value: number): number {
    const max = this.max();
    return max === 0 ? 0 : (value / max) * 85;
  }
}
