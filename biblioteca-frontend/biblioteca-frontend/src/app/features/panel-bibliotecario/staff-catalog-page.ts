import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AutorDTO, GeneroDTO, LibroDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-catalog-page',
  imports: [FormsModule, RouterLink, PanelTemplate, Card, Button, FormField, ConfirmDialog],
  template: `
    <app-panel-template titulo="Gestión de Catálogo, Autores y Géneros">
      <!-- Formularios rápidos de Géneros y Autores -->
      <div class="grid-cards" style="margin-bottom: 1.5rem;">
        <app-card>
          <h3>Crear Género Literario</h3>
          <form (ngSubmit)="crearGenero()" class="stack">
            <app-form-field etiqueta="Nombre del género" para="genNom" [obligatorio]="true" [error]="errGenero()">
              <input id="genNom" class="campo" placeholder="Ej. Ciencia Ficción" [(ngModel)]="nuevoGenero.nombre" name="genNom" />
            </app-form-field>
            <input class="campo" placeholder="Descripción (opcional)" [(ngModel)]="nuevoGenero.descripcion" name="genDesc" />
            <app-button tipo="submit" texto="Guardar Género" tamano="pequeno" />
          </form>
        </app-card>

        <app-card>
          <h3>Crear Autor</h3>
          <form (ngSubmit)="crearAutor()" class="stack">
            <app-form-field etiqueta="Nombre completo del autor" para="autNom" [obligatorio]="true" [error]="errAutor()">
              <input id="autNom" class="campo" placeholder="Ej. Gabriel García Márquez" [(ngModel)]="nuevoAutor.nombre" name="autNom" />
            </app-form-field>
            <input class="campo" placeholder="Nacionalidad (opcional)" [(ngModel)]="nuevoAutor.nacionalidad" name="autNac" />
            <app-button tipo="submit" texto="Guardar Autor" tamano="pequeno" />
          </form>
        </app-card>
      </div>

      <!-- Formulario Crear / Editar Libro (Pruebas 65, 66, 67) -->
      <app-card>
        <h3>{{ editandoId() ? 'Editar Libro #' + editandoId() : 'Registrar Nuevo Libro' }}</h3>
        <form (ngSubmit)="guardarLibro()" novalidate>
          <div class="form-grid">
            <app-form-field etiqueta="ISBN" para="isbn" [obligatorio]="true" [error]="erroresLibro()['isbn'] || null">
              <input id="isbn" class="campo" [(ngModel)]="formLibro.isbn" name="isbn" />
            </app-form-field>

            <app-form-field etiqueta="Título" para="tit" [obligatorio]="true" [error]="erroresLibro()['titulo'] || null">
              <input id="tit" class="campo" [(ngModel)]="formLibro.titulo" name="tit" />
            </app-form-field>

            <app-form-field etiqueta="Idioma" para="idi" [obligatorio]="true" [error]="erroresLibro()['idioma'] || null">
              <input id="idi" class="campo" [(ngModel)]="formLibro.idioma" name="idi" />
            </app-form-field>

            <app-form-field etiqueta="Año Publicación" para="anio" [obligatorio]="true" [error]="erroresLibro()['anio'] || null">
              <input id="anio" class="campo" type="number" [(ngModel)]="formLibro.anioPublicacion" name="anio" />
            </app-form-field>

            <app-form-field etiqueta="Editorial" para="edi">
              <input id="edi" class="campo" [(ngModel)]="formLibro.editorial" name="edi" />
            </app-form-field>

            <app-form-field etiqueta="Número de páginas" para="pag">
              <input id="pag" class="campo" type="number" [(ngModel)]="formLibro.numeroPaginas" name="pag" />
            </app-form-field>

            <app-form-field etiqueta="Género" para="gen" [obligatorio]="true" [error]="erroresLibro()['genero'] || null">
              <select id="gen" class="campo" [(ngModel)]="formLibro.generoId" name="gen">
                <option [ngValue]="0" disabled>Seleccione género...</option>
                @for (g of generos(); track g.id) { <option [ngValue]="g.id">{{ g.nombre }}</option> }
              </select>
            </app-form-field>

            <app-form-field etiqueta="Autor" para="aut" [obligatorio]="true" [error]="erroresLibro()['autor'] || null">
              <select id="aut" class="campo" [(ngModel)]="autorId" name="aut">
                <option [ngValue]="0" disabled>Seleccione autor...</option>
                @for (a of autores(); track a.id) { <option [ngValue]="a.id">{{ a.nombre }}</option> }
              </select>
            </app-form-field>
          </div>

          <app-form-field etiqueta="Sinopsis" para="sin">
            <textarea id="sin" class="campo" rows="2" [(ngModel)]="formLibro.sinopsis" name="sin"></textarea>
          </app-form-field>

          <div style="display: flex; gap: 0.65rem;">
            <app-button tipo="submit" [texto]="editandoId() ? 'Actualizar Libro' : 'Guardar Libro'" />
            @if (editandoId()) {
              <app-button texto="Cancelar Edición" variante="contorno" (presionado)="resetFormLibro()" />
            }
          </div>
        </form>
      </app-card>

      <!-- Listado de Libros -->
      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>ISBN</th><th>Título</th><th>Idioma</th><th>Año</th><th>Editorial</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (l of libros(); track l.id) {
              <tr>
                <td>{{ l.id }}</td>
                <td>{{ l.isbn }}</td>
                <td>{{ l.titulo }}</td>
                <td>{{ l.idioma }}</td>
                <td>{{ l.anioPublicacion }}</td>
                <td>{{ l.editorial || '-' }}</td>
                <td style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
                  <a [routerLink]="['/catalogo', l.id]" style="color: var(--color-primario); font-weight: 700; align-self: center;">Ver detalle</a>
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="cargarEdicion(l)" />
                  <app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="pedirEliminar(l)" />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmEliminarOpen"
        titulo="Eliminar Libro"
        [mensaje]="'¿Confirmas eliminar el libro ' + (libroAEliminar()?.titulo || '') + '? Si tiene préstamos o reservas pendientes será bloqueado.'"
        textoConfirmar="Sí, eliminar"
        [peligro]="true"
        (confirmar)="ejecutarEliminar()"
      />
    </app-panel-template>
  `,
  styles: [`
    .stack { display: flex; flex-direction: column; gap: 0.6rem; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.75rem; }
  `]
})
export class StaffCatalogPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly generos = signal<GeneroDTO[]>([]);
  protected readonly autores = signal<AutorDTO[]>([]);

  protected nuevoGenero: GeneroDTO = { nombre: '', descripcion: '' };
  protected nuevoAutor: AutorDTO = { nombre: '', nacionalidad: '' };
  protected readonly errGenero = signal<string | null>(null);
  protected readonly errAutor = signal<string | null>(null);

  protected readonly editandoId = signal<number | null>(null);
  protected readonly erroresLibro = signal<Record<string, string>>({});
  protected autorId = 0;
  protected formLibro: Partial<LibroDTO> = {
    isbn: '', titulo: '', idioma: 'Español', anioPublicacion: 2025,
    editorial: '', sinopsis: '', numeroPaginas: undefined, generoId: 0
  };

  protected readonly confirmEliminarOpen = signal(false);
  protected readonly libroAEliminar = signal<LibroDTO | null>(null);

  ngOnInit(): void {
    this.cargarTodo();
  }

  private cargarTodo(): void {
    this.catalogo.buscarLibros().subscribe((l) => this.libros.set(l));
    this.catalogo.listarGeneros().subscribe((g) => {
      this.generos.set(g);
      if (!this.formLibro.generoId && g[0]?.id) this.formLibro.generoId = g[0].id;
    });
    this.catalogo.listarAutores().subscribe((a) => {
      this.autores.set(a);
      if (!this.autorId && a[0]?.id) this.autorId = a[0].id;
    });
  }

  protected crearGenero(): void {
    if (!this.nuevoGenero.nombre.trim()) {
      this.errGenero.set('El nombre del género es obligatorio.');
      return;
    }
    this.errGenero.set(null);
    this.catalogo.crearGenero(this.nuevoGenero).subscribe({
      next: (g) => {
        this.ui.mostrar('Género creado.', 'exito');
        this.nuevoGenero = { nombre: '', descripcion: '' };
        this.cargarTodo();
        if (g.id) this.formLibro.generoId = g.id;
      },
      error: (e) => this.errGenero.set(extraerMensajeError(e)),
    });
  }

  protected crearAutor(): void {
    if (!this.nuevoAutor.nombre.trim()) {
      this.errAutor.set('El nombre del autor es obligatorio.');
      return;
    }
    this.errAutor.set(null);
    this.catalogo.crearAutor(this.nuevoAutor).subscribe({
      next: (a) => {
        this.ui.mostrar('Autor creado.', 'exito');
        this.nuevoAutor = { nombre: '', nacionalidad: '' };
        this.cargarTodo();
        if (a.id) this.autorId = a.id;
      },
      error: (e) => this.errAutor.set(extraerMensajeError(e)),
    });
  }

  protected cargarEdicion(l: LibroDTO): void {
    this.editandoId.set(l.id);
    this.erroresLibro.set({});
    this.formLibro = { ...l };
    this.autorId = l.autoresIds?.[0] ?? 0;
  }

  protected resetFormLibro(): void {
    this.editandoId.set(null);
    this.erroresLibro.set({});
    this.formLibro = {
      isbn: '', titulo: '', idioma: 'Español', anioPublicacion: 2025,
      editorial: '', sinopsis: '', numeroPaginas: undefined,
      generoId: this.generos()[0]?.id ?? 0
    };
  }

  protected guardarLibro(): void {
    const errs: Record<string, string> = {};
    if (!this.formLibro.isbn?.trim()) errs['isbn'] = 'El ISBN es obligatorio.';
    if (!this.formLibro.titulo?.trim()) errs['titulo'] = 'El título es obligatorio.';
    if (!this.formLibro.idioma?.trim()) errs['idioma'] = 'El idioma es obligatorio.';
    if (!this.formLibro.anioPublicacion) errs['anio'] = 'El año de publicación es obligatorio.';
    if (!this.formLibro.generoId || Number(this.formLibro.generoId) === 0) errs['genero'] = 'Selecciona un género.';
    if (!this.autorId || Number(this.autorId) === 0) errs['autor'] = 'Selecciona un autor.';

    this.erroresLibro.set(errs);
    if (Object.keys(errs).length > 0) return;

    const payload: Partial<LibroDTO> = {
      ...this.formLibro,
      generoId: Number(this.formLibro.generoId),
      autoresIds: [Number(this.autorId)],
    };

    const idEdit = this.editandoId();
    const obs = idEdit
      ? this.catalogo.actualizarLibro(idEdit, payload)
      : this.catalogo.crearLibro(payload);

    obs.subscribe({
      next: () => {
        this.ui.mostrar(idEdit ? 'Libro actualizado.' : 'Libro registrado en el catálogo.', 'exito');
        this.resetFormLibro();
        this.cargarTodo();
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected pedirEliminar(l: LibroDTO): void {
    this.libroAEliminar.set(l);
    this.confirmEliminarOpen.set(true);
  }

  protected ejecutarEliminar(): void {
    const l = this.libroAEliminar();
    if (!l) return;
    this.catalogo.eliminarLibro(l.id).subscribe({
      next: () => { this.ui.mostrar('Libro eliminado.', 'info'); this.cargarTodo(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
