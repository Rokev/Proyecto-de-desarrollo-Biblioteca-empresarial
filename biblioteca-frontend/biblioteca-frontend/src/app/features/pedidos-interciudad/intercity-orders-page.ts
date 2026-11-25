import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoInterciudadDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-intercity-orders-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, AccountTemplate, Card, Badge, Button, EmptyState, ConfirmDialog],
  template: `
    <app-account-template titulo="Mis Pedidos Interciudad">
      <p><a routerLink="/pedidos/nuevo" class="btn-nuevo">+ Solicitar nuevo pedido interciudad</a></p>
      @if (pedidos().length === 0) {
        <app-empty-state titulo="Sin pedidos interciudad" descripcion="Solicita un libro disponible en otra ciudad y recíbelo en casa." />
      } @else {
        <div class="grid-cards">
          @for (p of pedidos(); track p.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>Pedido #{{ p.id }}</strong>
                <app-badge [texto]="p.estado" [estado]="p.estado" />
              </div>
              <h3>{{ p.libroTitulo }}</h3>
              <p><strong>Sede devolución:</strong> {{ p.sedeDevolucionNombre }}</p>
              <p><strong>Recepción:</strong> {{ p.fechaRecepcion }} · {{ p.horaRecepcion }}</p>
              <p><strong>Envío:</strong> {{ p.costoEnvio | currency:'COP':'symbol-narrow':'1.0-0' }}</p>
              @if (p.fechaLimiteRecogida) {
                <p class="alerta-fecha">Recoger antes de: {{ p.fechaLimiteRecogida | date:'d MMM y, h:mm a' }}</p>
              }
              @if (p.fechaLimiteDevolucion) {
                <p><strong>Devolver antes de:</strong> {{ p.fechaLimiteDevolucion | date:'d MMM y' }}</p>
              }
              <div cardPie>
                <a [routerLink]="['/mi-cuenta/pedidos', p.id]" class="btn-nuevo">Ver detalle</a>
                @if (p.estado === 'PENDIENTE_PAGO') {
                  <a [routerLink]="['/pago', 'ENVIO_INTERCIUDAD', p.id]" [queryParams]="{ monto: p.costoEnvio }">Pagar envío</a>
                }
                <!-- Prueba 58: mostrar botón también en EN_TRANSITO para probar rechazo del backend -->
                @if (p.estado === 'SOLICITADO' || p.estado === 'PENDIENTE_PAGO' || p.estado === 'EN_TRANSITO') {
                  <app-button texto="Cancelar Pedido" variante="peligro" tamano="pequeno" (presionado)="pedirCancelar(p.id)" />
                }
              </div>
            </app-card>
          }
        </div>
      }

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Cancelar Pedido Interciudad"
        mensaje="¿Confirmas que deseas cancelar este pedido interciudad? Si está en estado SOLICITADO se reembolsará el costo de envío."
        textoConfirmar="Confirmar Cancelación"
        [peligro]="true"
        (confirmar)="ejecutarCancelar()"
      />
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; } .btn-nuevo { color: var(--color-primario); font-weight: 700; text-decoration: none; } .alerta-fecha { color: var(--color-error); font-weight: 700; }`]
})
export class InterCityOrdersPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly pedidos = signal<PedidoInterciudadDTO[]>([]);
  protected readonly confirmOpen = signal(false);
  protected readonly idSel = signal<number | null>(null);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ops.pedidosUsuario(this.auth.usuarioId()!).subscribe((p) => this.pedidos.set(p));
  }

  protected pedirCancelar(id: number): void {
    this.idSel.set(id);
    this.confirmOpen.set(true);
  }

  protected ejecutarCancelar(): void {
    const id = this.idSel();
    if (!id) return;
    this.ops.cancelarPedidoInterciudad(id, this.auth.usuarioId()!).subscribe({
      next: () => { this.ui.mostrar('Pedido cancelado y envío reembolsado.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
