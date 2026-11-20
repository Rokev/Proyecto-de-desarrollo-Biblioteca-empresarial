import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UsuarioDTO } from '../../core/models/biblioteca.models';
import { AdminService } from '../../core/services/admin.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-admin-users-page',
  imports: [FormsModule, PanelTemplate, Card, Badge, Button],
  template: `
    <app-panel-template titulo="Administración de Usuarios y Personal">
      <app-card>
        <h3>Crear Cuenta de Personal (Bibliotecario / Repartidor)</h3>
        <form (ngSubmit)="crear()" class="form-grid">
          <input class="campo" placeholder="Nombre *" [(ngModel)]="nuevo.nombre" name="nom" required />
          <input class="campo" placeholder="Apellido *" [(ngModel)]="nuevo.apellido" name="ape" required />
          <input class="campo" placeholder="Documento *" [(ngModel)]="nuevo.documento" name="doc" required />
          <input class="campo" placeholder="Usuario *" [(ngModel)]="nuevo.nombreUsuario" name="usr" required />
          <input class="campo" type="email" placeholder="Correo *" [(ngModel)]="nuevo.correo" name="cor" required />
          <input class="campo" type="password" placeholder="Contraseña *" [(ngModel)]="nuevo.contrasena" name="pas" required />
          <select class="campo" [(ngModel)]="nuevo.rolId" name="rol">
            @for (r of roles(); track r.id) { <option [ngValue]="r.id">{{ r.nombre }}</option> }
          </select>
          <input class="campo" type="number" placeholder="Sede ID (Bibliotecario)" [(ngModel)]="nuevo.sedeId" name="sed" />
          <app-button tipo="submit" texto="Crear Cuenta" />
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Nombre</th><th>Correo</th><th>Rol ID</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (u of usuarios(); track u.id) {
              <tr>
                <td>{{ u.id }}</td>
                <td>{{ u.nombre }} {{ u.apellido }}</td>
                <td>{{ u.correo }}</td>
                <td>{{ u.rolId }}</td>
                <td><app-badge [texto]="u.activo ? 'Activo' : 'Suspendido'" [estado]="u.activo ? 'ACTIVA' : 'VENCIDO'" /></td>
                <td style="display: flex; gap: 0.4rem;">
                  @if (u.activo) {
                    <app-button texto="Suspender" variante="peligro" tamano="pequeno" (presionado)="suspender(u.id)" />
                  } @else {
                    <app-button texto="Reactivar" tamano="pequeno" (presionado)="reactivar(u.id)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 0.65rem; }`]
})
export class AdminUsersPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly usuarios = signal<UsuarioDTO[]>([]);
  protected readonly roles = signal<{ id: number; nombre: string }[]>([]);
  protected nuevo: Partial<UsuarioDTO> = { nombre: '', apellido: '', documento: '', nombreUsuario: '', correo: '', contrasena: '', rolId: 2, activo: true };

  ngOnInit(): void {
    this.cargar();
    this.admin.listarRoles().subscribe((r) => this.roles.set(r));
  }

  private cargar(): void { this.admin.listarUsuarios().subscribe((u) => this.usuarios.set(u)); }

  protected crear(): void {
    this.admin.crearUsuario({ ...this.nuevo, activo: true }).subscribe({
      next: () => { this.ui.mostrar('Cuenta creada exitosamente.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected suspender(id: number): void {
    this.admin.suspenderUsuario(id).subscribe(() => { this.ui.mostrar('Cuenta suspendida.', 'advertencia'); this.cargar(); });
  }

  protected reactivar(id: number): void {
    this.admin.reactivarUsuario(id).subscribe(() => { this.ui.mostrar('Cuenta reactivada.', 'exito'); this.cargar(); });
  }
}
