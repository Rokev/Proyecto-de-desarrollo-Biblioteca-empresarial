import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AutorDTO, GeneroDTO, LibroDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-catalog-page',
  imports: [FormsModule, PanelTemplate, Card, Button],
  template: `
    <app-panel-template titulo="Gestión de Catálogo (Libros)">
      <app-card>
        <h3>Registrar Nuevo Libro</h3>
        <form (ngSubmit)="crear()" class="form-grid">
          <input class="campo" placeholder="ISBN *" [(ngModel)]="nuevo.isbn" name="isbn" required />
          <input class="campo" placeholder="Título *" [(ngModel)]="nuevo.titulo" name="tit" required />
          <input class="campo" placeholder="Idioma *" [(ngModel)]="nuevo.idioma" name="idi" required />
          <input class="campo" type="number" placeholder="Año *" [(ngModel)]="nuevo.anioPublicacion" name="anio" required />
          <select class="campo" [(ngModel)]="nuevo.generoId" name="gen">
            @for (g of generos(); track g.id) { <option [ngValue]="g.id">{{ g.nombre }}</option> }
          </select>
          <select class="campo" [(ngModel)]="autorId" name="aut">
            @for (a of autores(); track a.id) { <option [ngValue]="a.id">{{ a.nombre }}</option> }
          </select>
          <app-button tipo="submit" texto="Guardar Libro" />
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>ISBN</th><th>Título</th><th>Idioma</th><th>Año</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (l of libros(); track l.id) {
              <tr>
                <td>{{ l.id }}</td><td>{{ l.isbn }}</td><td>{{ l.titulo }}</td><td>{{ l.idioma }}</td><td>{{ l.anioPublicacion }}</td>
                <td><app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="eliminar(l.id)" /></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 0.75rem; }`]
})
export class StaffCatalogPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly generos = signal<GeneroDTO[]>([]);
  protected readonly autores = signal<AutorDTO[]>([]);
  protected autorId = 1;
  protected nuevo: Partial<LibroDTO> = { isbn: '', titulo: '', idioma: 'Español', anioPublicacion: 2025, generoId: 1 };

  ngOnInit(): void {
    this.cargar();
    this.catalogo.listarGeneros().subscribe((g) => { this.generos.set(g); this.nuevo.generoId = g[0]?.id ?? 1; });
    this.catalogo.listarAutores().subscribe((a) => { this.autores.set(a); this.autorId = a[0]?.id ?? 1; });
  }

  private cargar(): void { this.catalogo.buscarLibros().subscribe((l) => this.libros.set(l)); }

  protected crear(): void {
    this.catalogo.crearLibro({ ...this.nuevo, autoresIds: [Number(this.autorId)] }).subscribe({
      next: () => { this.ui.mostrar('Libro registrado.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected eliminar(id: number): void {
    this.catalogo.eliminarLibro(id).subscribe({
      next: () => { this.ui.mostrar('Libro eliminado.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
