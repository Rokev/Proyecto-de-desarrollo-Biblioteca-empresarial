import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MultaDTO, SancionDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { StatCard } from '../../shared/ui/molecules/stat-card/stat-card';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-fines-page',
  imports: [CurrencyPipe, DatePipe, AccountTemplate, Card, Badge, Button, StatCard],
  template: `
    <app-account-template titulo="Multas, Recargos y Sanciones">
      <div class="resumen">
        <app-stat-card
          etiqueta="Saldo pendiente seleccionado"
          [valor]="(totalSeleccionado() | currency:'COP':'symbol-narrow':'1.0-0') || '$ 0'"
          [alerta]="totalSeleccionado() > 0"
        />
        @if (seleccionadas().length > 0) {
          <app-button texto="Pagar Multas Seleccionadas" (presionado)="irAPagar()" />
        }
      </div>

      <h2>Multas y Recargos</h2>
      <div class="grid-cards">
        @for (m of multas(); track m.id) {
          <app-card>
            <div cardEncabezado class="head">
              <label class="chk">
                @if (m.estado === 'PENDIENTE') {
                  <input type="checkbox" [checked]="estaSeleccionada(m.id)" (change)="toggle(m.id)" />
                }
                <strong>Cargo #{{ m.id }}</strong>
              </label>
              <app-badge [texto]="m.estado" [estado]="m.estado" />
            </div>
            <p class="monto">{{ m.monto | currency:'COP':'symbol-narrow':'1.0-0' }}</p>
            <p>{{ m.motivo }}</p>
            <small>Aplicado: {{ m.fechaGeneracion | date:'d MMM y' }}</small>
          </app-card>
        }
      </div>

      <h2 style="margin-top: 2rem;">Sanciones Activas</h2>
      <div class="grid-cards">
        @for (s of sanciones(); track s.id) {
          <app-card [destacada]="s.estado === 'ACTIVA'">
            <div cardEncabezado class="head">
              <strong>{{ s.tipo }}</strong>
              <app-badge [texto]="s.estado" [estado]="s.estado" />
            </div>
            <p>{{ s.motivo }}</p>
            <small>Desde: {{ s.fechaAplicacion | date:'d MMM y' }} · Hasta: {{ s.fechaFin | date:'d MMM y' }}</small>
          </app-card>
        }
      </div>
    </app-account-template>
  `,
  styles: [`
    .resumen { display: flex; align-items: center; gap: 1.25rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
    .head { display: flex; justify-content: space-between; align-items: center; }
    .chk { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
    .monto { font-size: 1.35rem; font-weight: 700; margin: 0.35rem 0; }
  `]
})
export class FinesPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly multas = signal<MultaDTO[]>([]);
  protected readonly sanciones = signal<SancionDTO[]>([]);
  protected readonly seleccionadas = signal<number[]>([]);

  protected readonly totalSeleccionado = computed(() =>
    this.multas()
      .filter((m) => this.seleccionadas().includes(m.id))
      .reduce((acc, m) => acc + m.monto, 0)
  );

  ngOnInit(): void {
    const uid = this.auth.usuarioId()!;
    this.ops.multasUsuario(uid).subscribe((m) => this.multas.set(m));
    this.ops.sancionesUsuario(uid).subscribe((s) => this.sanciones.set(s));
  }

  protected estaSeleccionada(id: number): boolean { return this.seleccionadas().includes(id); }

  protected toggle(id: number): void {
    this.seleccionadas.update((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  protected irAPagar(): void {
    const ids = this.seleccionadas();
    if (ids.length === 0) return;
    this.router.navigate(['/pago', 'MULTA', ids[0]], {
      queryParams: { monto: this.totalSeleccionado(), ids: ids.join(',') },
    });
  }
}
