import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  EjemplarDTO,
  LibroDTO,
  PedidoInterciudadDTO,
  PrestamoDetalleDTO,
  UsuarioDTO,
} from '../../core/models/biblioteca.models';
import { AdminService } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-operations-page',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, PanelTemplate, Badge, Button, Card, Alert, ConfirmDialog],
  template: `
    <app-panel-template titulo="Préstamos y Pedidos Interciudad de Sede">
      @if (!auth.perfil()?.sedeId) {
        <app-alert titulo="Sin sede asignada" mensaje="Tu usuario no tiene una Sede ID asignada." tipo="error" />
      }

      <!-- Prueba 74: Filtros combinables por estado, usuario y libro -->
      <div class="filtros-bar">
        <select class="campo" style="max-width: 210px;" [ngModel]="fEstado()" (ngModelChange)="fEstado.set($event)">
          <option value="">Todos los estados</option>
          <option value="SOLICITADO">SOLICITADO</option>
          <option value="ACTIVO">ACTIVO</option>
          <option value="VENCIDO">VENCIDO</option>
          <option value="EN_TRANSITO">EN_TRANSITO</option>
          <option value="ENTREGADO">ENTREGADO</option>
          <option value="NO_RECIBIDO">NO_RECIBIDO</option>
          <option value="EN_ESPERA_DE_RECOGIDA">EN_ESPERA_DE_RECOGIDA</option>
          <option value="DEVUELTO_PENDIENTE_TRASLADO">DEVUELTO_PENDIENTE_TRASLADO</option>
          <option value="EN_CAMINO_ORIGEN">EN_CAMINO_ORIGEN</option>
          <option value="CANCELADO">CANCELADO</option>
          <option value="CERRADO">CERRADO</option>
        </select>
        <input class="campo" style="max-width: 220px;" placeholder="Buscar por usuario..." [ngModel]="fUsuario()" (ngModelChange)="fUsuario.set($event)" />
        <input class="campo" style="max-width: 220px;" placeholder="Buscar por libro..." [ngModel]="fLibro()" (ngModelChange)="fLibro.set($event)" />
        <app-button texto="Limpiar" variante="contorno" tamano="pequeno" (presionado)="limpiarFiltros()" />
      </div>

      <h3>Préstamos Locales de la Sede</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Usuario</th><th>Libro / Ejemplar</th><th>Estado</th><th>Vencimiento</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (p of prestamosFiltrados(); track p.prestamoId) {
              <tr>
                <td><a [routerLink]="['/panel/prestamos', p.prestamoId]" style="color:var(--color-primario); font-weight:700;">{{ p.prestamoId }}</a></td>
                <td>{{ nombreUsuario(p.usuarioId) }}</td>
                <td><strong>{{ p.libroTitulo || tituloPorEjemplar(p.ejemplarId) }}</strong></td>
                <td><app-badge [texto]="p.estado || ''" [estado]="p.estado || ''" /></td>
                <td>{{ p.fechaLimiteDevolucion ? (p.fechaLimiteDevolucion | date:'d MMM y') : 'Pendiente entrega' }}</td>
                <td style="display:flex; gap:0.35rem; flex-wrap:wrap;">
                  <a [routerLink]="['/panel/prestamos', p.prestamoId]" style="align-self:center; font-size:0.85rem; font-weight:700;">Ver</a>
                  @if (p.estado === 'SOLICITADO') {
                    <app-button texto="Entregar" tamano="pequeno" (presionado)="pedirAccion('ENTREGAR_LOCAL', p.prestamoId, p)" />
                  }
                  @if (p.estado === 'ACTIVO' || p.estado === 'VENCIDO') {
                    <app-button texto="Dañado" variante="peligro" tamano="pequeno" (presionado)="pedirAccion('DANADO', p.prestamoId, p)" />
                    <app-button texto="Perdido" variante="peligro" tamano="pequeno" (presionado)="pedirAccion('PERDIDO', p.prestamoId, p)" />
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6">No hay préstamos que coincidan con los filtros.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Pruebas 94, 95, 96, 102, 103 -->
      <h3 style="margin-top: 2rem;">Pedidos Interciudad (Salida / Retorno a Sede de Origen)</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Libro</th><th>Usuario / Dirección</th><th>Programado</th><th>Devolución / Envío</th><th>Motivo Retorno</th><th>Repartidor</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (ped of pedidosOrigenFiltrados(); track ped.id) {
              <tr>
                <td>{{ ped.id }}</td>
                <td><strong>{{ ped.libroTitulo }}</strong></td>
                <td>{{ nombreUsuario(ped.usuarioId) }}<br><small>{{ ped.direccionEntrega }} ({{ ped.ciudadEntrega }})</small></td>
                <td>{{ ped.fechaRecepcion }} · {{ ped.horaRecepcion }}</td>
                <td>{{ ped.sedeDevolucionNombre }}<br><small>{{ ped.costoEnvio | currency:'COP':'symbol-narrow':'1.0-0' }}</small></td>
                <td>{{ motivoRetorno(ped) }}</td>
                <td>{{ ped.repartidorNombre || '-' }}</td>
                <td><app-badge [texto]="ped.estado" [estado]="ped.estado" /></td>
                <td style="display:flex; gap:0.35rem; flex-wrap:wrap; align-items:center;">
                  <select
                    class="campo"
                    style="width: 155px;"
                    [disabled]="ped.estado !== 'SOLICITADO'"
                    [ngModel]="repartidorPorPedido[ped.id] || 0"
                    (ngModelChange)="repartidorPorPedido[ped.id] = $event"
                  >
                    <option [ngValue]="0">Repartidor...</option>
                    @for (r of repartidores(); track r.id) { <option [ngValue]="r.id">{{ r.nombre }} {{ r.apellido }}</option> }
                  </select>
                  <app-button
                    texto="Despachar"
                    tamano="pequeno"
                    [deshabilitado]="ped.estado !== 'SOLICITADO'"
                    [attr.title]="ped.estado !== 'SOLICITADO' ? 'Solo disponible en estado SOLICITADO' : 'Registrar envío al repartidor'"
                    (presionado)="pedirAccion('DESPACHAR', ped.id, ped)"
                  />
                  @if (ped.estado === 'EN_CAMINO_ORIGEN') {
                    <app-button texto="Recibir y Cerrar" variante="secundario" tamano="pequeno" (presionado)="pedirAccion('CERRAR_ORIGEN', ped.id, ped)" />
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="9">No hay pedidos salientes con los filtros aplicados.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Pruebas 97, 99, 100, 101 -->
      <h3 style="margin-top: 2rem;">Pedidos Interciudad (Sede de Devolución / Recogida)</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Libro</th><th>Usuario</th><th>Límite Recogida</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (ped of pedidosDestinoFiltrados(); track ped.id) {
              <tr>
                <td>{{ ped.id }}</td>
                <td><strong>{{ ped.libroTitulo }}</strong></td>
                <td>{{ nombreUsuario(ped.usuarioId) }}</td>
                <td>{{ ped.fechaLimiteRecogida ? (ped.fechaLimiteRecogida | date:'d MMM y, h:mm a') : '-' }}</td>
                <td><app-badge [texto]="ped.estado" [estado]="ped.estado" /></td>
                <td style="display:flex; gap:0.35rem; flex-wrap:wrap;">
                  @if (ped.estado === 'NO_RECIBIDO') {
                    <app-button texto="Recibir No Entregado" tamano="pequeno" (presionado)="pedirAccion('RECIBIR_NO_ENTREGADO', ped.id, ped)" />
                  }
                  <app-button
                    texto="Entregar en Sede"
                    tamano="pequeno"
                    [deshabilitado]="ped.estado !== 'EN_ESPERA_DE_RECOGIDA'"
                    [attr.title]="ped.estado !== 'EN_ESPERA_DE_RECOGIDA' ? 'Solo disponible en estado EN_ESPERA_DE_RECOGIDA' : 'Entregar al usuario en mostrador'"
                    (presionado)="pedirAccion('ENTREGAR_SEDE', ped.id, ped)"
                  />
                  @if (ped.estado === 'ENTREGADO') {
                    <app-button texto="Recibir Devolución" variante="secundario" tamano="pequeno" (presionado)="pedirAccion('DEVOLVER_SEDE', ped.id, ped)" />
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6">No hay pedidos de devolución con los filtros aplicados.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Pruebas 22, 77 y 98: intentar devolución interciudad por ID en esta sede -->
      <app-card style="margin-top: 2rem;">
        <h4>Buscar / Recibir Pedido Interciudad por ID (Cualquier Sede)</h4>
        <p><small>Permite intentar registrar la devolución de un pedido interciudad en esta sede y verificar la validación de sede elegida.</small></p>
        <div style="display: flex; gap: 0.5rem; max-width: 440px;">
          <input type="number" class="campo" placeholder="ID Pedido Interciudad" [(ngModel)]="pedidoManualId" />
          <app-button texto="Registrar Devolución" (presionado)="devolverManual()" />
        </div>
      </app-card>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="confirmTit()"
        [mensaje]="confirmMsg()"
        [peligro]="accionPendiente() === 'DANADO' || accionPendiente() === 'PERDIDO'"
        [mostrarInput]="accionPendiente() === 'DANADO' || accionPendiente() === 'PERDIDO'"
        etiquetaInput="Observaciones de la novedad"
        (confirmar)="ejecutarAccion($event)"
      />
    </app-panel-template>
  `,
  styles: [`.filtros-bar { display:flex; gap:0.65rem; flex-wrap:wrap; margin-bottom:1.25rem; }`]
})
export class StaffOperationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly admin = inject(AdminService);
  private readonly catalogo = inject(CatalogoService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly prestamos = signal<PrestamoDetalleDTO[]>([]);
  protected readonly pedidosOrigen = signal<PedidoInterciudadDTO[]>([]);
  protected readonly pedidosDestino = signal<PedidoInterciudadDTO[]>([]);
  protected readonly usuarios = signal<UsuarioDTO[]>([]);
  protected readonly roles = signal<{ id: number; nombre: string }[]>([]);
  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly ejemplares = signal<EjemplarDTO[]>([]);

  // Signals (no propiedades planas): los computed() de abajo solo se recalculan si cambia un signal
  protected readonly fEstado = signal('');
  protected readonly fUsuario = signal('');
  protected readonly fLibro = signal('');
  protected repartidorPorPedido: Record<number, number> = {};
  protected pedidoManualId: number | null = null;

  protected readonly confirmOpen = signal(false);
  protected readonly accionPendiente = signal<string>('');
  protected readonly idPendiente = signal<number>(0);
  protected readonly dataPendiente = signal<any>(null);
  protected readonly confirmTit = signal('');
  protected readonly confirmMsg = signal('');

  protected readonly repartidores = computed(() => {
    const rolRep = this.roles().find((r) => r.nombre.toUpperCase() === 'REPARTIDOR')?.id;
    return this.usuarios().filter((u) => u.activo && (!rolRep || u.rolId === rolRep));
  });

  protected readonly prestamosFiltrados = computed(() =>
    this.prestamos().filter((p) => {
      const estado = this.fEstado();
      const usuario = this.fUsuario().trim().toLowerCase();
      const libro = this.fLibro().trim().toLowerCase();
      if (estado && p.estado !== estado) return false;
      if (usuario && !this.nombreUsuario(p.usuarioId).toLowerCase().includes(usuario)) return false;
      if (libro && !(p.libroTitulo || this.tituloPorEjemplar(p.ejemplarId)).toLowerCase().includes(libro)) return false;
      return true;
    })
  );

  protected readonly pedidosOrigenFiltrados = computed(() => this.filtrarPedidos(this.pedidosOrigen()));

  protected readonly pedidosDestinoFiltrados = computed(() => this.filtrarPedidos(this.pedidosDestino()));

  private filtrarPedidos(lista: PedidoInterciudadDTO[]): PedidoInterciudadDTO[] {
    const estado = this.fEstado();
    const usuario = this.fUsuario().trim().toLowerCase();
    const libro = this.fLibro().trim().toLowerCase();
    return lista.filter((p) => {
      if (estado && p.estado !== estado) return false;
      if (usuario && !this.nombreUsuario(p.usuarioId).toLowerCase().includes(usuario)) return false;
      if (libro && !(p.libroTitulo ?? '').toLowerCase().includes(libro)) return false;
      return true;
    });
  }

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) return;
    this.ops.prestamosPorSede(sedeId).subscribe((p) => this.prestamos.set(p));
    this.ops.pedidosSedeOrigen(sedeId).subscribe((p) => this.pedidosOrigen.set(p));
    this.ops.pedidosSedeDevolucion(sedeId).subscribe((p) => this.pedidosDestino.set(p));
    this.catalogo.buscarLibros().subscribe((l) => this.libros.set(l));
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
    this.admin.listarRoles().subscribe({
      next: (r) => this.roles.set(r),
      error: (e: unknown) => this.avisarFalloLista('roles', e),
    });
    this.admin.listarUsuarios().subscribe({
      next: (u) => this.usuarios.set(u),
      error: (e: unknown) => this.avisarFalloLista('usuarios', e),
    });
  }

  private avisarFalloLista(recurso: string, e: unknown): void {
    const estado = (e as { status?: number })?.status ?? '?';
    console.error(`No se pudo cargar /${recurso}`, e);
    this.ui.mostrar(`No se pudo cargar la lista de ${recurso} (HTTP ${estado}). Los nombres y repartidores no estarán disponibles.`, 'advertencia');
  }

  protected limpiarFiltros(): void { this.fEstado.set(''); this.fUsuario.set(''); this.fLibro.set(''); }

  protected nombreUsuario(id: number): string {
    const u = this.usuarios().find((x) => x.id === id);
    return u ? `${u.nombre} ${u.apellido}` : `Usuario ${id}`;
  }

  protected tituloPorEjemplar(ejId: number): string {
    const ej = this.ejemplares().find((e) => e.id === ejId);
    if (!ej) return `Ejemplar ${ejId}`;
    const lib = this.libros().find((l) => l.id === ej.libroId);
    return lib ? `${lib.titulo} (${ej.codigoBarras})` : ej.codigoBarras;
  }

  protected motivoRetorno(p: PedidoInterciudadDTO): string {
    if (p.estado === 'EN_CAMINO_ORIGEN' || p.estado === 'CERRADO') {
      return p.fechaEntrega ? 'Devuelto por usuario' : 'No recogido a tiempo';
    }
    return '-';
  }

  protected pedirAccion(tipo: string, id: number, data: any): void {
    if (tipo === 'DESPACHAR' && (!this.repartidorPorPedido[id] || this.repartidorPorPedido[id] === 0)) {
      this.ui.mostrar('Debes seleccionar un repartidor antes de despachar.', 'advertencia');
      return;
    }
    this.accionPendiente.set(tipo);
    this.idPendiente.set(id);
    this.dataPendiente.set(data);
    this.confirmTit.set(`Confirmar acción: ${tipo.replace(/_/g, ' ')}`);
    this.confirmMsg.set(`¿Deseas ejecutar esta operación sobre el registro #${id}?`);
    this.confirmOpen.set(true);
  }

  protected ejecutarAccion(obs: string): void {
    const tipo = this.accionPendiente();
    const id = this.idPendiente();
    const data = this.dataPendiente();
    const bibId = this.auth.usuarioId()!;
    const sedeId = this.auth.perfil()?.sedeId!;

    if (tipo === 'ENTREGAR_LOCAL') {
      this.ops.obtenerPrestamo(id).pipe(
        switchMap((prestamo) => this.ops.actualizarPrestamo(id, { ...prestamo, estado: 'ACTIVO' })),
      ).subscribe({
        next: () => { this.ui.mostrar('Entrega registrada. Plazo iniciado.', 'exito'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (tipo === 'DANADO' || tipo === 'PERDIDO') {
      this.ops.registrarDanioPerdida(id, tipo, bibId, obs || undefined).subscribe({
        next: () => { this.ui.mostrar(`Ejemplar marcado como ${tipo}, stock actualizado y multa aplicada.`, 'advertencia'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (tipo === 'DESPACHAR') {
      const repId = Number(this.repartidorPorPedido[id]);
      this.ops.despacharPedido(id, bibId, repId).subscribe({
        next: () => { this.ui.mostrar('Pedido despachado al repartidor. Stock descontado y notificaciones enviadas.', 'exito'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (tipo === 'RECIBIR_NO_ENTREGADO') {
      this.ops.recepcionNoRecibido(id, bibId).subscribe({
        next: () => { this.ui.mostrar('Recepción registrada: En espera de recogida.', 'info'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (tipo === 'ENTREGAR_SEDE') {
      this.ops.entregaEnSede(id, bibId).subscribe({
        next: () => { this.ui.mostrar('Entregado al usuario en sede. Plazo de devolución iniciado.', 'exito'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (tipo === 'DEVOLVER_SEDE') {
      this.ops.devolucionEnSede(id, bibId, sedeId).subscribe({
        next: () => { this.ui.mostrar('Devolución recibida, pendiente de traslado a sede de origen.', 'exito'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else if (tipo === 'CERRAR_ORIGEN') {
      this.ops.cerrarEnOrigen(id, bibId).subscribe({
        next: () => { this.ui.mostrar('Retorno recibido, stock repuesto en sede de origen y pedido cerrado.', 'exito'); this.cargar(); },
        error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    }
  }

  protected devolverManual(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId || !this.pedidoManualId) return;
    this.ops.devolucionEnSede(this.pedidoManualId, this.auth.usuarioId()!, sedeId).subscribe({
      next: () => { this.ui.mostrar('Devolución interciudad registrada.', 'exito'); this.cargar(); },
      error: (e: unknown) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}