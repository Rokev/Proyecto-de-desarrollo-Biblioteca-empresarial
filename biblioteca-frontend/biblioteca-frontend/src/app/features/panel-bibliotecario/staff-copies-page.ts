import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EjemplarDTO, LibroDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-copies-page',
  imports: [FormsModule, PanelTemplate, Card, Badge, Button, FormField],
  template: `
    <app-panel-template titulo="Gestión de Ejemplares y Stock por Sede" subtitulo="Registra copias físicas o actualiza su disponibilidad.">
      <app-card>
        <h3>Registrar Nuevo Ejemplar (Aumentar Stock)</h3>
        <form (ngSubmit)="crear()" class="form-grid">
          <app-form-field etiqueta="Libro" para="libSel" [obligatorio]="true">
            <select id="libSel" class="campo" [(ngModel)]="libroId" name="libSel">
              @for (l of libros(); track l.id) { <option [ngValue]="l.id">{{ l.titulo }} ({{ l.isbn }})</option> }
            </select>
          </app-form-field>

          <app-form-field etiqueta="Sede" para="sedeSel" [obligatorio]="true">
            <select id="sedeSel" class="campo" [(ngModel)]="sedeId" name="sedeSel">
              @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option> }
            </select>
          </app-form-field>

          <app-form-field etiqueta="Código de Barras / Inventario" para="cod" [obligatorio]="true">
            <input id="cod" class="campo" [(ngModel)]="codigoBarras" name="cod" placeholder="Ej. EJ-00101" required />
          </app-form-field>

          <app-form-field etiqueta="Estado Inicial" para="est">
            <select id="est" class="campo" [(ngModel)]="estado" name="est">
              <option value="DISPONIBLE">DISPONIBLE</option>
              <option value="RESERVADO">RESERVADO</option>
              <option value="DANADO">DANADO</option>
            </select>
          </app-form-field>

          <div style="align-self: end; margin-bottom: 0.9rem;">
            <app-button tipo="submit" texto="Registrar Ejemplar" />
          </div>
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Código</th><th>Libro</th><th>Sede</th><th>Estado</th><th>Cambiar Estado / Stock</th></tr></thead>
          <tbody>
            @for (e of ejemplares(); track e.id) {
              <tr>
                <td>{{ e.id }}</td>
                <td><strong>{{ e.codigoBarras }}</strong></td>
                <td>{{ nombreLibro(e.libroId) }}</td>
                <td>{{ nombreSede(e.sedeId) }}</td>
                <td><app-badge [texto]="e.estado" [estado]="e.estado" /></td>
                <td style="display: flex; gap: 0.4rem;">
                  @if (e.estado !== 'DISPONIBLE') {
                    <app-button texto="Marcar Disponible" tamano="pequeno" (presionado)="cambiarEstado(e, 'DISPONIBLE')" />
                  }
                  @if (e.estado === 'DISPONIBLE') {
                    <app-button texto="Marcar Dañado" variante="peligro" tamano="pequeno" (presionado)="cambiarEstado(e, 'DANADO')" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.75rem; }`]
})
export class StaffCopiesPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly ejemplares = signal<EjemplarDTO[]>([]);
  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);

  protected libroId = 0;
  protected sedeId = 0;
  protected codigoBarras = '';
  protected estado: EjemplarDTO['estado'] = 'DISPONIBLE';

  ngOnInit(): void {
    this.cargar();
    this.catalogo.buscarLibros().subscribe((l) => { this.libros.set(l); this.libroId = l[0]?.id ?? 0; });
    this.catalogo.listarTodasSedes().subscribe((s) => {
      this.sedes.set(s);
      this.sedeId = this.auth.perfil()?.sedeId ?? (s[0]?.id ?? 0);
    });
  }

  private cargar(): void {
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
  }

  protected nombreLibro(id: number): string {
    return this.libros().find((l) => l.id === id)?.titulo ?? `Libro #${id}`;
  }

  protected nombreSede(id: number): string {
    return this.sedes().find((s) => s.id === id)?.nombre ?? `Sede #${id}`;
  }

  protected crear(): void {
    if (!this.codigoBarras.trim() || !this.libroId || !this.sedeId) {
      this.ui.mostrar('Completa libro, sede y código de barras.', 'advertencia');
      return;
    }
    this.catalogo.crearEjemplar({
      codigoBarras: this.codigoBarras.trim(),
      libroId: Number(this.libroId),
      sedeId: Number(this.sedeId),
      estado: this.estado,
    }).subscribe({
      next: () => {
        this.ui.mostrar('Ejemplar registrado y stock actualizado.', 'exito');
        this.codigoBarras = '';
        this.cargar();
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected cambiarEstado(e: EjemplarDTO, nuevoEstado: EjemplarDTO['estado']): void {
    this.catalogo.actualizarEjemplar(e.id!, { ...e, estado: nuevoEstado }).subscribe({
      next: () => {
        this.ui.mostrar(`Estado actualizado a ${nuevoEstado}.`, 'exito');
        this.cargar();
      },
      error: (err) => this.ui.mostrar(extraerMensajeError(err), 'error'),
    });
  }
}
