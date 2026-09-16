const LOCALE = 'es-CO';

const dateFormatter = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', year: 'numeric' });
const shortDateFormatter = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short' });
const dateTimeFormatter = new Intl.DateTimeFormat(LOCALE, {
  day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit'
});
const relativeFormatter = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });

/** "2026-09-16" -> Date local (sin corrimiento de zona horaria). */
export function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Date -> "2026-09-16" */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** "2026-09-16T14:05:00" (hora local del servidor) -> Date */
export function parseDateTime(value: string | null | undefined): Date | null {
  return value ? new Date(value) : null;
}

export function formatDate(value: string | null | undefined): string {
  const date = parseDateTime(value);
  return date ? dateFormatter.format(date) : '—';
}

export function formatDateTime(value: string | null | undefined): string {
  const date = parseDateTime(value);
  return date ? dateTimeFormatter.format(date) : '—';
}

/** "2026-09-16" -> "16 sept" (sin el "de" que agrega el formato en español) */
export function formatShortDay(value: string): string {
  const parts = shortDateFormatter.formatToParts(parseLocalDate(value));
  const day = parts.find(part => part.type === 'day')?.value ?? '';
  const month = (parts.find(part => part.type === 'month')?.value ?? '').replace('.', '');
  return `${day} ${month}`.trim();
}

/** "hace 5 minutos", "ayer", "hace 3 días"... */
export function formatRelative(value: string | null | undefined, now: Date = new Date()): string {
  const date = parseDateTime(value);
  if (!date) {
    return 'Sin actividad';
  }
  const seconds = Math.round((date.getTime() - now.getTime()) / 1000);
  const abs = Math.abs(seconds);
  if (abs < 60) {
    return relativeFormatter.format(seconds, 'second');
  }
  if (abs < 3600) {
    return relativeFormatter.format(Math.round(seconds / 60), 'minute');
  }
  if (abs < 86400) {
    return relativeFormatter.format(Math.round(seconds / 3600), 'hour');
  }
  if (abs < 86400 * 30) {
    return relativeFormatter.format(Math.round(seconds / 86400), 'day');
  }
  return dateFormatter.format(date);
}
