import { HttpErrorResponse } from '@angular/common/http';

/** Devuelve el mensaje que envía el backend ({ message }) o un texto por defecto. */
export function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const message = error.error?.message;
    if (typeof message === 'string' && message.trim().length > 0) {
      return message;
    }
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
  }
  return fallback;
}
