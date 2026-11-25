import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { LibroDTO, ReservaLibroDTO, UsuarioDTO } from '../../core/models/biblioteca.models';
import { AdminService } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-reservations-page',
  imports: [DatePipe, PanelTemplate, Badge],
  template: `
    <app-panel-template titulo="Reservas Activas de la Sede">
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Usuario</th><th>Libro</th><th>Fecha Reserva</th><th>Límite Retiro</th><th>Estado</th></tr></thead>
          <tbody>
            @for (r of reservas(); track r.id) {
              <tr>
                <td>#{{ r.id }}</td>
                <td>{{ nombreUsuario(r.usuarioId) }}</td>
                <td>{{ tituloLibro(r.libroId) }}</td>
                <td>{{ r.fechaReserva | date:'d MMM y, h:mm a' }}</td>
                <td>{{ r.fechaLimiteRetiro ? (r.fechaLimiteRetiro | date:'d MMM y, h:mm a') : '-' }}</td>
                <td><app-badge [texto]="r.estado || ''" [estado]="r.estado || ''" /></td>
              </tr>
            } @empty {
              <tr><td colspan="6">No hay reservas activas en esta sede.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `
})
export class StaffReservationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly catalogo = inject(CatalogoService);
  private readonly admin = inject(AdminService);

  protected readonly reservas = signal<ReservaLibroDTO[]>([]);
  protected readonly usuarios = signal<UsuarioDTO[]>([]);
  protected readonly libros = signal<LibroDTO[]>([]);

  ngOnInit(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) return;
    this.ops.reservasLibroSede(sedeId).subscribe((r) => this.reservas.set(r.filter((x) => x.estado !== 'CANCELADA')));
    this.catalogo.buscarLibros().subscribe((l) => this.libros.set(l));
    this.admin.listarUsuarios().subscribe({ next: (u) => this.usuarios.set(u), error: () => {} });
  }

  protected nombreUsuario(id: number): string {
    const u = this.usuarios().find((x) => x.id === id);
    return u ? `${u.nombre} ${u.apellido}` : `Usuario #${id}`;
  }

  protected tituloLibro(id: number): string {
    return this.libros().find((l) => l.id === id)?.titulo ?? `Libro #${id}`;
  }
}
