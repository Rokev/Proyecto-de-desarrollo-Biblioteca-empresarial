import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { CargaService } from '../services/carga.service';
import { SIN_INDICADOR_CARGA } from './contexto';

export const cargaInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(SIN_INDICADOR_CARGA)) return next(req);
  const carga = inject(CargaService);
  carga.iniciar();
  return next(req).pipe(finalize(() => carga.terminar()));
};
