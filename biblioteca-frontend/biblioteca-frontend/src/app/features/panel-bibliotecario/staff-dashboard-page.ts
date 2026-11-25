import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoInterciudadDTO, PrestamoDTO, ReservaLibroDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { StatCard } from '../../shared/ui/molecules/stat-card/stat-card';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-dashboard-page',
  imports: [RouterLink, PanelTemplate, StatCard, Alert],
  template: `
    <app-panel-template titulo="Panel de Gestión de Sede" subtitulo="Resumen operativo en tiempo real.">
      @if (!auth.perfil()?.sedeId) {
        <app-alert
          titulo="Bibliotecario sin sede asignada"
          mensaje="Tu cuenta aún no tiene una sede vinculada. Solicita al administrador que asigne tu Sede ID."
          tipo="advertencia"
        />
      } @else {
        <div class="grid-cards">
          <app-stat-card etiqueta="Préstamos de la Sede" [valor]="prestamos().length" />
          <app-stat-card etiqueta="Reservas Pendientes" [valor]="reservas().length" />
          <app-stat-card etiqueta="Pedidos Interciudad Salientes" [valor]="pedidosSalida().length" />
        </div>
      }
      <div class="accesos">
        <a routerLink="/panel/devoluciones" class="card-link">📷 Confirmar Devolución por QR</a>
        <a routerLink="/panel/prestamos" class="card-link">📦 Gestionar Entregas y Pedidos</a>
        <a routerLink="/panel/libros" class="card-link">📚 Catálogo, Autores y Géneros</a>
        <a routerLink="/panel/ejemplares" class="card-link">🏷️ Ejemplares y Stock</a>
      </div>
    </app-panel-template>
  `,
  styles: [`
    .accesos { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; margin-top: 1.5rem; }
    .card-link { background: var(--color-superficie); border: 1px solid var(--color-borde); padding: 1.25rem; border-radius: var(--radio-lg); text-decoration: none; color: var(--color-primario); font-weight: 700; box-shadow: var(--sombra-sm); }
  `]
})
export class StaffDashboardPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  protected readonly auth = inject(AuthService);

  protected readonly prestamos = signal<PrestamoDTO[]>([]);
  protected readonly reservas = signal<ReservaLibroDTO[]>([]);
  protected readonly pedidosSalida = signal<PedidoInterciudadDTO[]>([]);

  ngOnInit(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) return;
    this.ops.prestamosPorSede(sedeId).subscribe((p) => this.prestamos.set(p));
    this.ops.reservasLibroSede(sedeId).subscribe((r) => this.reservas.set(r));
    this.ops.pedidosSedeOrigen(sedeId).subscribe((pe) => this.pedidosSalida.set(pe));
  }
}
