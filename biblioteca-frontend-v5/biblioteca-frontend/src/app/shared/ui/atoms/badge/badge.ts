import { Component, computed, input } from '@angular/core';

export type VarianteBadge = 'exito' | 'info' | 'advertencia' | 'error' | 'neutro';

const MAPA: Record<string, VarianteBadge> = {
  DISPONIBLE: 'exito', DEVUELTO: 'exito', ACTIVA: 'exito', ACTIVO: 'exito',
  CONFIRMADA: 'exito', ENTREGADO: 'exito', PAGADA: 'exito', APROBADO: 'exito', CERRADO: 'exito',
  SOLICITADO: 'info', EN_ESPERA: 'info', EN_ESPERA_RECOGIDA: 'info', NOTIFICADA: 'info',
  EN_TRANSITO: 'info', EN_CAMINO_ORIGEN: 'info', PROGRAMADO: 'info',
  PENDIENTE_PAGO: 'advertencia', PENDIENTE: 'advertencia', RESERVADO: 'advertencia',
  DEVUELTO_PENDIENTE_TRASLADO: 'advertencia',
  VENCIDO: 'error', CANCELADO: 'error', DANADO: 'error', PERDIDO: 'error',
  RECHAZADA: 'error', RECHAZADO: 'error', FUERA_DE_SERVICIO: 'error', NO_RECIBIDO: 'error',
};

@Component({
  selector: 'app-badge',
  template: `<span class="badge" [class]="'badge badge--' + varianteResuelta()">{{ texto() }}</span>`,
  styles: [`
    .badge { display: inline-block; padding: 0.2rem 0.6rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
    .badge--exito { background: var(--color-exito-bg); color: var(--color-exito); }
    .badge--info { background: var(--color-info-bg); color: var(--color-info); }
    .badge--advertencia { background: var(--color-advertencia-bg); color: var(--color-advertencia); }
    .badge--error { background: var(--color-error-bg); color: var(--color-error); }
    .badge--neutro { background: #e2e8f0; color: #334155; }
  `]
})
export class Badge {
  readonly texto = input.required<string>();
  readonly estado = input('');
  readonly variante = input<VarianteBadge | null>(null);

  protected readonly varianteResuelta = computed<VarianteBadge>(() => {
    if (this.variante()) return this.variante()!;
    const key = (this.estado() || this.texto()).toUpperCase().replace(/\s+/g, '_');
    return MAPA[key] ?? 'neutro';
  });
}
