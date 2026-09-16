/** Un valor con identidad (segmento de dona o de barra apilada). */
export interface ChartSegment {
  key: string;
  label: string;
  value: number;
  /** Color CSS, normalmente una variable: var(--status-pending) */
  color: string;
}

export interface LegendItem {
  key: string;
  label: string;
  color: string;
}

export interface StackedRow {
  id: number;
  label: string;
  /** Texto pequeño bajo la etiqueta (rol, última actividad...) */
  meta?: string;
  badge?: string;
  muted?: boolean;
  segments: ChartSegment[];
}

export interface BarItem {
  key: string;
  label: string;
  value: number;
}

export interface LineSeries {
  key: string;
  label: string;
  color: string;
  values: number[];
}

export interface TooltipState {
  x: number;
  y: number;
  /** above: centrado sobre el punto (por defecto); right/left: al lado de una línea vertical */
  placement?: 'above' | 'right' | 'left';
  title: string;
  rows: { label: string; value: string; color: string }[];
}

/** Número "bonito" para el máximo de un eje con 4 divisiones. */
export function niceMax(max: number, divisions = 4): { max: number; step: number } {
  if (max <= divisions) {
    return { max: divisions, step: 1 };
  }
  const rough = max / divisions;
  const magnitude = Math.pow(10, Math.floor(Math.log10(rough)));
  const normalized = rough / magnitude;
  const niceNormalized = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  const step = niceNormalized * magnitude;
  return { max: step * divisions, step };
}

export function percentOf(value: number, total: number): string {
  if (total === 0) {
    return '0%';
  }
  const pct = (value / total) * 100;
  return formatPercent(pct < 10 && pct > 0 ? Math.round(pct * 10) / 10 : Math.round(pct));
}

const numberFormatter = new Intl.NumberFormat('es-CO');
const percentFormatter = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });

export function formatNumber(value: number): string {
  return numberFormatter.format(value);
}

/** 40.9 -> "40,9 %" en formato colombiano, sin espacio: "40,9%" */
export function formatPercent(value: number): string {
  return `${percentFormatter.format(value)}%`;
}
