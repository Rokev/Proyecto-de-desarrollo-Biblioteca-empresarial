import { HttpErrorResponse } from '@angular/common/http';

export function extraerMensajeError(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Sin conexión con el servidor. Verifica que la API esté activa.';
    }
    const body = err.error;
    if (body && typeof body === 'object') {
      if (typeof body.mensaje === 'string') return body.mensaje;
      if (typeof body.detail === 'string') return body.detail;
      if (typeof body.message === 'string') return body.message;
      if (Array.isArray(body.errores) && body.errores.length > 0) {
        return body.errores.join(' · ');
      }
    }
    return `Error ${err.status}: ${err.statusText || 'Solicitud fallida'}`;
  }
  return 'Ocurrió un error inesperado.';
}
