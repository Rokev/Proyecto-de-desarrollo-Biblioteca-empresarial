import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { PedidoInterciudadDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-delivery-board-page',
  imports: [PanelTemplate, Card, Badge, Button, EmptyState, ConfirmDialog, Alert],
  template: `
    <app-panel-template titulo="Tablero del Repartidor" subtitulo="Entregas a domicilio asignadas y traslados de retorno pendientes.">
      @if (avisoNoRecibido()) {
        <app-alert titulo="Pedido registrado como NO RECIBIDO" [mensaje]="avisoNoRecibido()!" tipo="advertencia" />
      }

      <h2>1. Entregas a Domicilio Asignadas (En Tránsito)</h2>
      @if (entregas().length === 0) {
        <app-empty-state icono="🚚" titulo="Sin entregas a domicilio" descripcion="No tienes pedidos en tránsito asignados." />
      } @else {
        <div class="grid-cards">
          @for (p of entregas(); track p.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>Pedido #{{ p.id }}</strong>
                <app-badge [texto]="p.estado" [estado]="p.estado" />
              </div>
              <h3>{{ p.libroTitulo }}</h3>
              <p><strong>Destinatario:</strong> Usuario #{{ p.usuarioId }}</p>
              <p><strong>Dirección:</strong> {{ p.direccionEntrega }} ({{ p.ciudadEntrega }})</p>
              <p><strong>Programado:</strong> {{ p.fechaRecepcion }} · {{ p.horaRecepcion }}</p>
              <p><strong>Sede devolución (si no recibe):</strong> {{ p.sedeDevolucionNombre }}</p>
              <div cardPie>
                <app-button texto="Confirmar Entrega" tamano="pequeno" (presionado)="pedir(p, 'ENTREGAR')" />
                <app-button texto="Registrar No Recibido" variante="peligro" tamano="pequeno" (presionado)="pedir(p, 'NO_RECIBIDO')" />
              </div>
            </app-card>
          }
        </div>
      }

      <h2 style="margin-top: 2rem;">2. Traslados de Retorno a Sede de Origen</h2>
      @if (traslados().length === 0) {
        <p style="color: var(--color-texto-apoyo);">No hay traslados de retorno pendientes.</p>
      } @else {
        <div class="grid-cards">
          @for (p of traslados(); track p.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>Retorno #{{ p.id }}</strong>
                <app-badge [texto]="p.estado" [estado]="p.estado" />
              </div>
              <h3>{{ p.libroTitulo }}</h3>
              <p><strong>Recoger en:</strong> {{ p.sedeDevolucionNombre }}</p>
              <p><strong>Llevar a sede origen:</strong> {{ p.sedeOrigenNombre }}</p>
              <div cardPie>
                <app-button texto="Recoger y Trasladar a Origen" variante="secundario" tamano="pequeno" (presionado)="pedir(p, 'TRASLADAR')" />
              </div>
            </app-card>
          }
        </div>
      }

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="confirmTit()"
        [mensaje]="confirmMsg()"
        [peligro]="accion() === 'NO_RECIBIDO'"
        (confirmar)="ejecutar()"
      />
    </app-panel-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class DeliveryBoardPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly asignaciones = signal<PedidoInterciudadDTO[]>([]);
  protected readonly entregas = computed(() => this.asignaciones().filter((p) => p.estado === 'EN_TRANSITO'));
  protected readonly traslados = computed(() => this.asignaciones().filter((p) => p.estado === 'DEVUELTO_PENDIENTE_TRASLADO'));

  protected readonly confirmOpen = signal(false);
  protected readonly pedidoSel = signal<PedidoInterciudadDTO | null>(null);
  protected readonly accion = signal<'ENTREGAR' | 'NO_RECIBIDO' | 'TRASLADAR'>('ENTREGAR');
  protected readonly confirmTit = signal('');
  protected readonly confirmMsg = signal('');
  protected readonly avisoNoRecibido = signal<string | null>(null);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ops.asignacionesRepartidor(this.auth.usuarioId()!).subscribe((a) => this.asignaciones.set(a));
  }

  protected pedir(p: PedidoInterciudadDTO, acc: 'ENTREGAR' | 'NO_RECIBIDO' | 'TRASLADAR'): void {
    this.pedidoSel.set(p); this.accion.set(acc);
    if (acc === 'ENTREGAR') {
      this.confirmTit.set('Confirmar Entrega a Domicilio');
      this.confirmMsg.set(`¿Confirmas haber entregado "${p.libroTitulo}" en ${p.direccionEntrega}?`);
    } else if (acc === 'NO_RECIBIDO') {
      this.confirmTit.set('Registrar Pedido No Recibido');
      this.confirmMsg.set(`Se aplicará el recargo fijo al usuario y deberás llevar el libro a la sede ${p.sedeDevolucionNombre}.`);
    } else {
      this.confirmTit.set('Iniciar Traslado a Sede de Origen');
      this.confirmMsg.set(`¿Confirmas la recogida en ${p.sedeDevolucionNombre} para llevarlo a ${p.sedeOrigenNombre}?`);
    }
    this.confirmOpen.set(true);
  }

  protected ejecutar(): void {
    const p = this.pedidoSel(); if (!p) return;
    const uid = this.auth.usuarioId()!;
    const acc = this.accion();
    if (acc === 'ENTREGAR') {
      this.ops.confirmarEntregaRepartidor(uid, p.id).subscribe({
        next: () => { this.ui.mostrar('Entrega confirmada al usuario.', 'exito'); this.cargar(); },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (acc === 'NO_RECIBIDO') {
      this.ops.registrarNoRecibidoRepartidor(uid, p.id).subscribe({
        next: () => {
          this.avisoNoRecibido.set(`Debes llevar el libro "${p.libroTitulo}" (Pedido #${p.id}) a la sede de devolución: ${p.sedeDevolucionNombre}.`);
          this.cargar();
        },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else {
      this.ops.iniciarTrasladoRetorno(uid, p.id).subscribe({
        next: () => { this.ui.mostrar('Traslado hacia sede de origen iniciado.', 'info'); this.cargar(); },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    }
  }
}
