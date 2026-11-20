import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SedeDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-admin-sedes-page',
  imports: [FormsModule, PanelTemplate, Card, Badge, Button],
  template: `
    <app-panel-template titulo="Gestión de Sedes / Bibliotecas">
      <app-card>
        <h3>Registrar Nueva Sede</h3>
        <form (ngSubmit)="crear()" class="form-grid">
          <input class="campo" placeholder="Nombre *" [(ngModel)]="nueva.nombre" name="nom" required />
          <input class="campo" placeholder="Ciudad *" [(ngModel)]="nueva.ciudad" name="ciu" required />
          <input class="campo" placeholder="Dirección *" [(ngModel)]="nueva.direccion" name="dir" required />
          <app-button tipo="submit" texto="Crear Sede" />
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Nombre</th><th>Ciudad</th><th>Dirección</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (s of sedes(); track s.id) {
              <tr>
                <td>{{ s.id }}</td><td>{{ s.nombre }}</td><td>{{ s.ciudad }}</td><td>{{ s.direccion }}</td>
                <td><app-badge [texto]="s.enOperacion ? 'Activa' : 'Fuera de servicio'" [estado]="s.enOperacion ? 'ACTIVA' : 'FUERA_DE_SERVICIO'" /></td>
                <td>
                  @if (s.enOperacion) {
                    <app-button texto="Desactivar" variante="peligro" tamano="pequeno" (presionado)="desactivar(s.id)" />
                  } @else {
                    <app-button texto="Reactivar" tamano="pequeno" (presionado)="reactivar(s.id)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 0.65rem; }`]
})
export class AdminSedesPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly sedes = signal<SedeDTO[]>([]);
  protected nueva: Partial<SedeDTO> = { nombre: '', ciudad: '', direccion: '', enOperacion: true };

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.catalogo.listarTodasSedes().subscribe((s) => this.sedes.set(s)); }

  protected crear(): void {
    this.catalogo.crearSede({ ...this.nueva, enOperacion: true }).subscribe({
      next: () => { this.ui.mostrar('Sede registrada.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected desactivar(id: number): void {
    this.catalogo.desactivarSede(id).subscribe({
      next: () => { this.ui.mostrar('Sede desactivada.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected reactivar(id: number): void {
    this.catalogo.reactivarSede(id).subscribe({
      next: () => { this.ui.mostrar('Sede reactivada.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
