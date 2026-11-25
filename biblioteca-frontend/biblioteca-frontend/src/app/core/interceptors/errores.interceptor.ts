import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificacionesUiService } from '../services/notificaciones-ui.service';
import { SIN_NOTIFICACION_ERROR } from './contexto';

export const erroresInterceptor: HttpInterceptorFn = (req, next) => {
  const ui = inject(NotificacionesUiService);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !req.context.get(SIN_NOTIFICACION_ERROR)) {
        if (error.status === 0) {
          ui.mostrar('No se pudo conectar con la API. Verifica que Spring Boot esté encendido.', 'error');
        } else if (error.status >= 500) {
          ui.mostrar(`Error interno del servidor (${error.status}).`, 'error');
        }
      }
      return throwError(() => error);
    })
  );
};
