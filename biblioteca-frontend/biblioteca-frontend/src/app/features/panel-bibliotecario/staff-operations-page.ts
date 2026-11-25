import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PedidoInterciudadDTO, PrestamoDTO, UsuarioDTO } from '../../core/models/biblioteca.models';
import { AdminService } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-operations-page',
  imports: [FormsModule, PanelTemplate, Badge, Button, Card, Alert],
  template: `
    <app-panel-template titulo="Préstamos y Pedidos Interciudad de Sede">
      @if (!auth.perfil()?.sedeId) {
        <app-alert titulo="Sin sede asignada" mensaje="Tu usuario no tiene una Sede ID asignada." tipo="error" />
      }

      <h3>Préstamos Locales</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Usuario</th><th>Ejemplar</th><th>Estado</th><th>Acción</th></tr></thead>
          <tbody>
            @for (p of prestamos(); track p.id) {
              <tr>
                <td>#{{ p.id }}</td>
                <td>#{{ p.usuarioId }}</td>
                <td>#{{ p.ejemplarId }}</td>
                <td><app-badge [texto]="p.estado || ''" [estado]="p.estado || ''" /></td>
                <td>
                  @if (p.estado === 'SOLICITADO') {
                    <app-button texto="Registrar Entrega Mostrador" tamano="pequeno" (presionado)="entregarLocal(p)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <h3 style="margin-top: 2rem;">Pedidos Interciudad (Salida de Sede)</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Libro</th><th>Destino</th><th>Estado</th><th>Despacho</th></tr></thead>
          <tbody>
            @for (ped of pedidosOrigen(); track ped.id) {
              <tr>
                <td>#{{ ped.id }}</td>
                <td>{{ ped.libroTitulo }}</td>
                <td>{{ ped.ciudadEntrega }} ({{ ped.direccionEntrega }})</td>
                <td><app-badge [texto]="ped.estado" [estado]="ped.estado" /></td>
                <td>
                  @if (ped.estado === 'SOLICITADO') {
                    <select class="campo" style="width: 160px; display: inline-block; margin-right: 0.4rem;" [(ngModel)]="repartidorSel">
                      @for (r of repartidores(); track r.id) { <option [ngValue]="r.id">{{ r.nombre }}</option> }
                    </select>
                    <app-button texto="Despachar" tamano="pequeno" (presionado)="despachar(ped.id)" />
                  }
                  @if (ped.estado === 'EN_CAMINO_ORIGEN') {
                    <app-button texto="Recibir Retorno y Cerrar" tamano="pequeno" (presionado)="cerrarOrigen(ped.id)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <h3 style="margin-top: 2rem;">Pedidos Interciudad (Sede de Devolución / Recogida)</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Libro</th><th>Usuario</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (ped of pedidosDestino(); track ped.id) {
              <tr>
                <td>#{{ ped.id }}</td>
                <td>{{ ped.libroTitulo }}</td>
                <td>#{{ ped.usuarioId }}</td>
                <td><app-badge [texto]="ped.estado" [estado]="ped.estado" /></td>
                <td style="display: flex; gap: 0.4rem;">
                  @if (ped.estado === 'NO_RECIBIDO') {
                    <app-button texto="Recibir No Entregado" tamano="pequeno" (presionado)="recibirNoEntregado(ped.id)" />
                  }
                  @if (ped.estado === 'EN_ESPERA_RECOGIDA') {
                    <app-button texto="Entregar en Sede" tamano="pequeno" (presionado)="entregarSede(ped.id)" />
                  }
                  @if (ped.estado === 'ENTREGADO') {
                    <app-button texto="Recibir Devolución" tamano="pequeno" (presionado)="devolverSede(ped.id)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Prueba 22 / 77 / 98: intentar registrar devolución interciudad por ID en esta sede -->
      <app-card style="margin-top: 2rem;">
        <h4>Recepción Manual de Pedido Interciudad por ID</h4>
        <p><small>Ingresa el ID de un pedido interciudad para registrar su devolución en tu sede actual.</small></p>
        <div style="display: flex; gap: 0.5rem; max-width: 400px;">
          <input type="number" class="campo" placeholder="ID Pedido Interciudad" [(ngModel)]="pedidoManualId" />
          <app-button texto="Registrar Devolución" (presionado)="devolverManual()" />
        </div>
      </app-card>
    </app-panel-template>
  `
})
export class StaffOperationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly admin = inject(AdminService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly prestamos = signal<PrestamoDTO[]>([]);
  protected readonly pedidosOrigen = signal<PedidoInterciudadDTO[]>([]);
  protected readonly pedidosDestino = signal<PedidoInterciudadDTO[]>([]);
  protected readonly repartidores = signal<UsuarioDTO[]>([]);
  protected repartidorSel = 0;
  protected pedidoManualId: number | null = null;

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) return;
    this.ops.prestamosPorSede(sedeId).subscribe((p) => this.prestamos.set(p));
    this.ops.pedidosSedeOrigen(sedeId).subscribe((p) => this.pedidosOrigen.set(p));
    this.ops.pedidosSedeDevolucion(sedeId).subscribe((p) => this.pedidosDestino.set(p));
    this.admin.listarUsuarios().subscribe((u) => {
      const reps = u.filter((x) => x.activo);
      this.repartidores.set(reps);
      this.repartidorSel = reps[0]?.id ?? 0;
    });
  }

  protected entregarLocal(p: PrestamoDTO): void {
    this.ops.actualizarPrestamo(p.id!, { ...p, estado: 'ACTIVO' }).subscribe({
      next: () => { this.ui.mostrar('Entrega registrada. Plazo iniciado.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected despachar(id: number): void {
    this.ops.despacharPedido(id, this.auth.usuarioId()!, Number(this.repartidorSel)).subscribe({
      next: () => { this.ui.mostrar('Pedido despachado al repartidor.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected recibirNoEntregado(id: number): void {
    this.ops.recepcionNoRecibido(id, this.auth.usuarioId()!).subscribe({
      next: () => { this.ui.mostrar('En espera de recogida.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected entregarSede(id: number): void {
    this.ops.entregaEnSede(id, this.auth.usuarioId()!).subscribe({
      next: () => { this.ui.mostrar('Entregado al usuario en sede.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected devolverSede(id: number): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) return;
    this.ops.devolucionEnSede(id, this.auth.usuarioId()!, sedeId).subscribe({
      next: () => { this.ui.mostrar('Devolución recibida, pendiente de traslado.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected devolverManual(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId || !this.pedidoManualId) return;
    this.ops.devolucionEnSede(this.pedidoManualId, this.auth.usuarioId()!, sedeId).subscribe({
      next: () => { this.ui.mostrar('Devolución interciudad registrada.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected cerrarOrigen(id: number): void {
    this.ops.cerrarEnOrigen(id, this.auth.usuarioId()!).subscribe({
      next: () => { this.ui.mostrar('Stock repuesto y pedido cerrado.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
