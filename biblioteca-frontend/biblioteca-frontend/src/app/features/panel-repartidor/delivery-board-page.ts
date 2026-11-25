import { Component, inject, OnInit, signal } from '@angular/core';
import { PedidoInterciudadDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-delivery-board-page',
  imports: [PanelTemplate, Card, Badge, Button, EmptyState],
  template: `
    <app-panel-template titulo="Tablero del Repartidor" subtitulo="Entregas a domicilio asignadas y traslados de retorno pendientes.">
      @if (asignaciones().length === 0) {
        <app-empty-state titulo="Sin entregas pendientes" descripcion="No tienes pedidos en tránsito ni traslados asignados actualmente." />
      } @else {
        <div class="grid-cards">
          @for (p of asignaciones(); track p.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>Pedido #{{ p.id }}</strong>
                <app-badge [texto]="p.estado" [estado]="p.estado" />
              </div>
              <h3>{{ p.libroTitulo }}</h3>
              <p><strong>Dirección:</strong> {{ p.direccionEntrega }} ({{ p.ciudadEntrega }})</p>
              <p><strong>Programado:</strong> {{ p.fechaRecepcion }} · {{ p.horaRecepcion }}</p>
              <p><strong>Ruta retorno:</strong> {{ p.sedeDevolucionNombre }} → {{ p.sedeOrigenNombre }}</p>
              <div cardPie>
                @if (p.estado === 'EN_TRANSITO') {
                  <app-button texto="Confirmar Entrega" tamano="pequeno" (presionado)="entregar(p.id)" />
                  <app-button texto="Registrar No Recibido" variante="peligro" tamano="pequeno" (presionado)="noRecibido(p.id)" />
                }
                @if (p.estado === 'DEVUELTO_PENDIENTE_TRASLADO') {
                  <app-button texto="Recoger y Trasladar a Origen" variante="secundario" tamano="pequeno" (presionado)="trasladar(p.id)" />
                }
              </div>
            </app-card>
          }
        </div>
      }
    </app-panel-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class DeliveryBoardPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly asignaciones = signal<PedidoInterciudadDTO[]>([]);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ops.asignacionesRepartidor(this.auth.usuarioId()!).subscribe((a) => this.asignaciones.set(a));
  }

  protected entregar(pedidoId: number): void {
    this.ops.confirmarEntregaRepartidor(this.auth.usuarioId()!, pedidoId).subscribe({
      next: () => { this.ui.mostrar('Entrega confirmada al usuario.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected noRecibido(pedidoId: number): void {
    this.ops.registrarNoRecibidoRepartidor(this.auth.usuarioId()!, pedidoId).subscribe({
      next: () => { this.ui.mostrar('Registrado como no recibido. Recargo aplicado.', 'advertencia'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected trasladar(pedidoId: number): void {
    this.ops.iniciarTrasladoRetorno(this.auth.usuarioId()!, pedidoId).subscribe({
      next: () => { this.ui.mostrar('Traslado hacia sede de origen iniciado.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
