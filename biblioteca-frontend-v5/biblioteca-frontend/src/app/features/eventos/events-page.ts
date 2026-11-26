import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EventoDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { TruncarPipe } from '../../shared/pipes/truncar.pipe';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Card } from '../../shared/ui/atoms/card/card';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-events-page',
  imports: [FormsModule, RouterLink, DatePipe, MainTemplate, Card, Badge, TruncarPipe],
  template: `
    <app-main-template titulo="Eventos Culturales y Académicos" subtitulo="Participa en talleres, clubes de lectura y conferencias.">
      <!-- Prueba 36: filtro por sede, fecha y tipo -->
      <div class="filtros-bar">
        <select class="campo" [(ngModel)]="sedeFiltro">
          <option [ngValue]="0">Todas las sedes</option>
          @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }}</option> }
        </select>
        <input type="date" class="campo" [(ngModel)]="fechaFiltro" />
        <select class="campo" [(ngModel)]="tipoFiltro">
          <option value="TODOS">Todos los eventos</option>
          <option value="GENERAL">Abiertos al público</option>
          <option value="EXCLUSIVO">Exclusivos Suscriptores</option>
        </select>
      </div>

      <div class="grid-cards">
        @for (ev of eventosFiltrados(); track ev.id) {
          <app-card [destacada]="ev.exclusivo">
            <div cardEncabezado class="head">
              <app-badge [texto]="ev.exclusivo ? 'Exclusivo Suscriptores' : 'General'" [variante]="ev.exclusivo ? 'advertencia' : 'info'" />
              <small>{{ ev.cuposDisponibles }} / {{ ev.cupoMaximo }} cupos</small>
            </div>
            <h3>{{ ev.titulo }}</h3>
            <p><small>{{ ev.fechaInicio | date:'d MMM y, h:mm a' }} · {{ nombreSede(ev.sedeId) }}</small></p>
            <p>{{ ev.descripcion | truncar:100 }}</p>
            <div cardPie>
              <a [routerLink]="['/eventos', ev.id]">Ver detalle e inscribirme →</a>
            </div>
          </app-card>
        }
      </div>
    </app-main-template>
  `,
  styles: [`.filtros-bar { display: flex; gap: 1rem; margin-bottom: 1.25rem; flex-wrap: wrap; max-width: 680px; } .head { display: flex; justify-content: space-between; align-items: center; } a { color: var(--color-primario); font-weight: 700; text-decoration: none; }`]
})
export class EventsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly catalogo = inject(CatalogoService);

  protected readonly eventos = signal<EventoDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected sedeFiltro = 0;
  protected fechaFiltro = '';
  protected tipoFiltro = 'TODOS';

  protected readonly eventosFiltrados = computed(() =>
    this.eventos().filter((e) => {
      if (this.sedeFiltro && e.sedeId !== Number(this.sedeFiltro)) return false;
      if (this.fechaFiltro && !e.fechaInicio.startsWith(this.fechaFiltro)) return false;
      if (this.tipoFiltro === 'EXCLUSIVO' && !e.exclusivo) return false;
      if (this.tipoFiltro === 'GENERAL' && e.exclusivo) return false;
      return true;
    })
  );

  ngOnInit(): void {
    this.ops.listarEventosProximos().subscribe((e) => this.eventos.set(e));
    this.catalogo.listarSedesActivas().subscribe((s) => this.sedes.set(s));
  }

  protected nombreSede(id: number): string {
    return this.sedes().find((s) => s.id === id)?.nombre ?? `Sede #${id}`;
  }
}
