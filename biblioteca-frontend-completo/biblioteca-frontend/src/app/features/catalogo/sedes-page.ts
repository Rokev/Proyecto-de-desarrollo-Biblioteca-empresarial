import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SalaEstudioDTO, SedeDTO } from '../../core/models/biblioteca.models';
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
            <p>
              <strong>Salas de estudio:</strong>
              {{ contarSalas(sede.id) > 0 ? contarSalas(sede.id) + ' salas disponibles' : 'No cuenta con salas' }}
            </p>
            @if (contarSalas(sede.id) > 0) {
              <div cardPie>
                <a [routerLink]="['/sedes', sede.id, 'salas']">Ver y reservar salas →</a>
              </div>
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
  protected readonly salas = signal<SalaEstudioDTO[]>([]);

  ngOnInit(): void {
    this.catalogo.listarSedesActivas().subscribe((s) => this.sedes.set(s));
    this.ops.salasPorSede(0).subscribe((sa) => this.salas.set(sa));
  }

  protected contarSalas(sedeId: number): number {
    return this.salas().filter((s) => s.sedeId === sedeId && s.activa).length;
  }
}
