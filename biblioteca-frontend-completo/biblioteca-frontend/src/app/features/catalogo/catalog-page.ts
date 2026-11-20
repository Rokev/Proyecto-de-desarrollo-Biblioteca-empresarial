import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GeneroDTO, LibroDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { TruncarPipe } from '../../shared/pipes/truncar.pipe';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-catalog-page',
  imports: [FormsModule, RouterLink, MainTemplate, Card, Badge, Button, EmptyState, TruncarPipe],
  template: `
    <app-main-template titulo="Catálogo de Libros" subtitulo="Explora el acervo disponible en toda nuestra red de bibliotecas.">
      <div class="catalog-layout">
        <aside class="filtros" aria-label="Filtros de búsqueda">
          <h3>Filtrar catálogo</h3>
          <input class="campo" placeholder="Título..." [(ngModel)]="titulo" (keyup.enter)="buscar()" />
          <input class="campo" placeholder="Autor..." [(ngModel)]="autor" (keyup.enter)="buscar()" />
          <input class="campo" placeholder="Idioma (ej. Español)..." [(ngModel)]="idioma" />
          <input class="campo" type="number" placeholder="Año publicación..." [(ngModel)]="anio" />
          <select class="campo" [(ngModel)]="generoId">
            <option [ngValue]="null">Todos los géneros</option>
            @for (g of generos(); track g.id) { <option [ngValue]="g.id">{{ g.nombre }}</option> }
          </select>
          <select class="campo" [(ngModel)]="sedeId">
            <option [ngValue]="null">Todas las sedes activas</option>
            @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option> }
          </select>
          <select class="campo" [(ngModel)]="disponible">
            <option [ngValue]="null">Cualquier disponibilidad</option>
            <option [ngValue]="true">Con stock disponible</option>
            <option [ngValue]="false">Sin stock (para reservar)</option>
          </select>
          <div class="filtros__btns">
            <app-button texto="Buscar" (presionado)="buscar()" />
            <app-button texto="Limpiar" variante="contorno" (presionado)="limpiar()" />
          </div>
        </aside>

        <div>
          @if (libros().length === 0) {
            <app-empty-state
              titulo="Sin coincidencias"
              descripcion="No encontramos libros con los filtros seleccionados."
              textoAccion="Ver todo el catálogo"
              (accion)="limpiar()"
            />
          } @else {
            <div class="grid-cards">
              @for (libro of libros(); track libro.id) {
                <app-card>
                  <div cardEncabezado class="book-head">
                    <app-badge [texto]="libro.idioma" />
                    <small>ISBN: {{ libro.isbn }}</small>
                  </div>
                  <h3>{{ libro.titulo }}</h3>
                  <p class="meta">Año: {{ libro.anioPublicacion }} · {{ libro.editorial || 'Editorial General' }}</p>
                  <p class="sinopsis">{{ libro.sinopsis | truncar:110 }}</p>
                  <div cardPie>
                    <a [routerLink]="['/catalogo', libro.id]" class="btn-link">Ver ejemplares y solicitar</a>
                  </div>
                </app-card>
              }
            </div>
          }
        </div>
      </div>
    </app-main-template>
  `,
  styles: [`
    .catalog-layout { display: grid; grid-template-columns: 270px 1fr; gap: 1.5rem; align-items: start; }
    @media (max-width: 820px) { .catalog-layout { grid-template-columns: 1fr; } }
    .filtros { background: var(--color-superficie); padding: 1.15rem; border-radius: var(--radio-lg); border: 1px solid var(--color-borde); display: flex; flex-direction: column; gap: 0.65rem; }
    .filtros__btns { display: flex; gap: 0.5rem; margin-top: 0.4rem; }
    .book-head { display: flex; justify-content: space-between; align-items: center; }
    .meta { font-size: 0.85rem; color: var(--color-texto-apoyo); margin: 0.25rem 0 0.5rem; }
    .sinopsis { font-size: 0.9rem; margin: 0; }
    .btn-link { color: var(--color-primario); font-weight: 700; text-decoration: none; }
  `]
})
export class CatalogPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);

  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly generos = signal<GeneroDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);

  protected titulo = '';
  protected autor = '';
  protected idioma = '';
  protected anio: number | null = null;
  protected generoId: number | null = null;
  protected sedeId: number | null = null;
  protected disponible: boolean | null = null;

  ngOnInit(): void {
    this.catalogo.listarGeneros().subscribe((g) => this.generos.set(g));
    this.catalogo.listarSedesActivas().subscribe((s) => this.sedes.set(s));
    this.buscar();
  }

  protected buscar(): void {
    this.catalogo.buscarLibros({
      titulo: this.titulo,
      autor: this.autor,
      idioma: this.idioma,
      anio: this.anio,
      generoId: this.generoId,
      sedeId: this.sedeId,
      disponible: this.disponible,
    }).subscribe((res) => this.libros.set(res));
  }

  protected limpiar(): void {
    this.titulo = ''; this.autor = ''; this.idioma = '';
    this.anio = null; this.generoId = null; this.sedeId = null; this.disponible = null;
    this.buscar();
  }
}
