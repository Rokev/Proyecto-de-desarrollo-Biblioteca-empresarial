import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-subscription-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, AccountTemplate, Card, Badge, Button, EmptyState, ConfirmDialog],
  template: `
    <app-account-template titulo="Mi Suscripción">
      @if (auth.suscripcionVigente(); as sub) {
        <app-card [destacada]="true">
          <div cardEncabezado class="head">
            <strong>Suscripción #{{ sub.id }}</strong>
            <app-badge [texto]="sub.estado" [estado]="sub.estado" />
          </div>
          <p><strong>Vigencia:</strong> {{ sub.fechaInicio | date:'d MMM y' }} hasta {{ sub.fechaFin | date:'d MMM y' }}</p>
          <p><strong>Valor mensual:</strong> {{ sub.valorMensual | currency:'COP':'symbol-narrow':'1.0-0' }}</p>
          <p><strong>Descuento en salas:</strong> {{ sub.descuentoSalas }}%</p>
          <p><small>Si cancelas, conservarás tus beneficios hasta el fin del período ({{ sub.fechaFin | date:'d MMM y' }}) y se respetarán los pedidos interciudad en curso.</small></p>
          @if (sub.estado === 'ACTIVA') {
            <div cardPie>
              <app-button texto="Cancelar Suscripción" variante="peligro" (presionado)="confirmOpen.set(true)" />
            </div>
          }
        </app-card>

        <app-confirm-dialog
          [(abierto)]="confirmOpen"
          titulo="Cancelar Suscripción"
          mensaje="¿Deseas cancelar la renovación de tu suscripción? Conservarás tus beneficios hasta finalizar el período vigente."
          textoConfirmar="Confirmar Cancelación"
          [peligro]="true"
          (confirmar)="ejecutarCancelar(sub.id)"
        />
      } @else {
        <app-empty-state titulo="Sin suscripción activa" descripcion="Adquiere un plan para disfrutar pedidos interciudad y descuentos." />
        <p><a routerLink="/planes">Ver planes disponibles →</a></p>
      }
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class MySubscriptionPage {
  protected readonly auth = inject(AuthService);
  private readonly ops = inject(OperacionesService);
  private readonly ui = inject(NotificacionesUiService);
  protected readonly confirmOpen = signal(false);

  protected ejecutarCancelar(id: number): void {
    this.ops.cancelarSuscripcion(id).subscribe(() => {
      this.ui.mostrar('Suscripción cancelada. Tus beneficios siguen activos hasta el fin del período.', 'info');
      this.auth.refrescarSuscripcion(this.auth.usuarioId()!);
    });
  }
}
