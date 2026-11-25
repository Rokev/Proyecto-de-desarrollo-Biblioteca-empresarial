import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoInterciudadDTO } from '../../core/models/biblioteca.models';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Card } from '../../shared/ui/atoms/card/card';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-intercity-order-detail-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, AccountTemplate, Card, Badge],
  template: `
    <app-account-template titulo="Detalle de Pedido Interciudad">
      <p><a routerLink="/mi-cuenta/pedidos">← Volver a mis pedidos</a></p>
      @if (pedido(); as p) {
        <app-card>
          <div cardEncabezado class="head">
            <strong>Pedido #{{ p.id }} · {{ p.libroTitulo }}</strong>
            <app-badge [texto]="p.estado" [estado]="p.estado" />
          </div>
          <p><strong>Sede de Origen:</strong> {{ p.sedeOrigenNombre }}</p>
          <p><strong>Sede de Devolución Elegida:</strong> {{ p.sedeDevolucionNombre }}</p>
          <p><strong>Dirección de Entrega:</strong> {{ p.direccionEntrega }} ({{ p.ciudadEntrega }})</p>
          <p><strong>Fecha y Hora de Recepción Programada:</strong> {{ p.fechaRecepcion }} a las {{ p.horaRecepcion }}</p>
          <p><strong>Costo de Envío:</strong> {{ p.costoEnvio | currency:'COP':'symbol-narrow':'1.0-0' }}</p>
          @if (p.repartidorNombre) {
            <p><strong>Repartidor Asignado:</strong> {{ p.repartidorNombre }}</p>
          }
          @if (p.fechaLimiteRecogida) {
            <p style="color: var(--color-error); font-weight: 700;">
              Fecha límite para recoger en sede de devolución: {{ p.fechaLimiteRecogida | date:'d MMM y, h:mm a' }}
            </p>
          }
          @if (p.fechaLimiteDevolucion) {
            <p style="color: var(--color-primario); font-weight: 700;">
              Fecha límite de devolución: {{ p.fechaLimiteDevolucion | date:'d MMM y, h:mm a' }}
            </p>
          }
        </app-card>
      }
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; align-items: center; }`]
})
export class InterCityOrderDetailPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  readonly pedidoId = input.required({ transform: numberAttribute });
  protected readonly pedido = signal<PedidoInterciudadDTO | null>(null);

  ngOnInit(): void {
    this.ops.obtenerPedidoInterciudad(this.pedidoId()).subscribe((p) => this.pedido.set(p));
  }
}
