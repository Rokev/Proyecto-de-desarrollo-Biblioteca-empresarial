import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DisponibilidadSalasDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Card } from '../../shared/ui/atoms/card/card';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-sedes-page',
  imports: [RouterLink, MainTemplate, Card, Badge],
  template: `
    <app-main-template titulo="Bibliotecas en Funcionamiento" subtitulo="Consulta nuestras sedes activas y disponibilidad de salas de estudio.">
      <div class="grid-cards">
        @for (sede of sedes(); track sede.id) {
          <app-card>
            <div cardEncabezado class="head">
              <strong>{{ sede.ciudad }}</strong>
              <app-badge texto="En Operación" estado="ACTIVA" />
            </div>
            <h3>{{ sede.nombre }}</h3>
            <p>{{ sede.direccion }}</p>
            <p><small>Horario: {{ sede.horaApertura || '08:00' }} - {{ sede.horaCierre || '20:00' }}</small></p>
            @if (dispMapa().get(sede.id); as disp) {
              <p>
                <strong>Salas de estudio:</strong>
                @if (disp.tieneSalas) {
                  <span>{{ disp.salasDisponibles }} libres de {{ disp.totalSalas }} salas</span>
                } @else {
                  <span>No cuenta con salas de estudio</span>
                }
              </p>
              @if (disp.tieneSalas) {
                <div cardPie>
                  <a [routerLink]="['/sedes', sede.id, 'salas']">Ver disponibilidad y reservar →</a>
                </div>
              }
            }
          </app-card>
        }
      </div>
    </app-main-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; align-items: center; } a { color: var(--color-primario); font-weight: 700; text-decoration: none; }`]
})
export class SedesPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);

  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly dispMapa = signal<Map<number, DisponibilidadSalasDTO>>(new Map());

  ngOnInit(): void {
    this.catalogo.listarSedesActivas().subscribe((sedes) => {
      this.sedes.set(sedes);
      sedes.forEach((s) => {
        this.ops.disponibilidadSalas(s.id).subscribe((d) => {
          this.dispMapa.update((m) => {
            const copy = new Map(m);
            copy.set(s.id, d);
            return copy;
          });
        });
      });
    });
  }
}
