import { CurrencyPipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { PlanSuscripcionDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-plans-page',
  imports: [CurrencyPipe, MainTemplate, Card, Badge, Button, Alert],
  template: `
    <app-main-template titulo="Planes de Suscripción" subtitulo="Accede a pedidos interciudad a domicilio, descuentos en salas y eventos exclusivos.">
      @if (motivo() === 'suscripcion') {
        <app-alert titulo="Suscripción requerida" mensaje="Necesitas un plan vigente para acceder a pedidos interciudad." tipo="advertencia" />
      }
      @if (error()) { <app-alert titulo="Aviso" [mensaje]="error()!" tipo="error" /> }

      <div class="grid-cards">
        @for (plan of planes(); track plan.id) {
          <app-card [destacada]="true">
            <div cardEncabezado class="head">
              <strong>{{ plan.nombre }}</strong>
              <app-badge [texto]="plan.duracionMeses + ' mes(es)'" variante="info" />
            </div>
            <p class="precio">{{ plan.precio | currency:'COP':'symbol-narrow':'1.0-0' }}</p>
            <p>{{ plan.descripcion }}</p>
            <p><strong>Beneficios:</strong> {{ plan.beneficios }}</p>
            <p><strong>Descuento en salas:</strong> {{ plan.descuentoSalas }}%</p>
            <div cardPie>
              <app-button texto="Adquirir Plan" (presionado)="adquirir(plan)" />
            </div>
          </app-card>
        }
      </div>
    </app-main-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; align-items: center; } .precio { font-family: var(--font-heading); font-size: 1.75rem; font-weight: 700; color: var(--color-primario); margin: 0.5rem 0; }`]
})
export class PlansPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly motivo = input<string>();
  protected readonly planes = signal<PlanSuscripcionDTO[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.ops.listarPlanes().subscribe((p) => this.planes.set(p.filter((x) => x.activo)));
  }

  protected adquirir(plan: PlanSuscripcionDTO): void {
    if (!this.auth.autenticado()) { this.router.navigate(['/login']); return; }
    this.ops.comprarSuscripcion(this.auth.usuarioId()!, plan.id, true).subscribe({
      next: () => {
        this.router.navigate(['/pago', 'SUSCRIPCION', plan.id], { queryParams: { monto: plan.precio } });
      },
      error: (e) => this.error.set(extraerMensajeError(e)),
    });
  }
}
