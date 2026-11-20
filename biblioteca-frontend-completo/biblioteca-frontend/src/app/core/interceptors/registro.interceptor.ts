import { HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { finalize, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export const registroInterceptor: HttpInterceptorFn = (req, next) => {
  if (environment.production) return next(req);
  const inicio = performance.now();
  let status = '...';

  return next(req).pipe(
    tap({
      next: (ev) => {
        if (ev.type === HttpEventType.Response) status = String(ev.status);
      },
      error: (err) => {
        status = String(err.status ?? 'ERR');
      },
    }),
    finalize(() => {
      const ms = Math.round(performance.now() - inicio);
      console.info(`[HTTP] ${req.method} ${req.urlWithParams} -> ${status} (${ms} ms)`);
    })
  );
};
