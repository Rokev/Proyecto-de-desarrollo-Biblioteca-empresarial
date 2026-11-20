import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SalaEstudioDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-rooms-page',
  imports: [FormsModule, CurrencyPipe, MainTemplate, Card, Badge, Button, EmptyState],
  template: `
    <app-main-template titulo="Salas de Estudio" subtitulo="Reserva un espacio tranquilo. Los suscriptores reciben descuento automático.">
      <app-card>
        <div class="rango">
          <label>Inicio: <input type="datetime-local" class="campo" [(ngModel)]="fechaInicio" /></label>
          <label>Fin: <input type="datetime-local" class="campo" [(ngModel)]="fechaFin" /></label>
        </div>
      </app-card>

      @if (salasSede().length === 0) {
        <app-empty-state titulo="Sin salas disponibles" descripcion="Esta sede no cuenta con salas de estudio activas." />
      } @else {
        <div class="grid-cards" style="margin-top: 1.25rem;">
          @for (s of salasSede(); track s.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>{{ s.nombre }}</strong>
                <app-badge texto="Activa" estado="ACTIVA" />
              </div>
              <p><strong>Capacidad:</strong> {{ s.capacidad }} personas</p>
              <p><strong>Tarifa:</strong> {{ s.tarifaPorHora | currency:'COP':'symbol-narrow':'1.0-0' }} / hora</p>
              <p>{{ s.descripcion }}</p>
              <div cardPie>
                <app-button texto="Reservar Sala" (presionado)="reservar(s.id)" />
              </div>
            </app-card>
          }
        </div>
      }
    </app-main-template>
  `,
  styles: [`.rango { display: flex; gap: 1rem; flex-wrap: wrap; } .head { display: flex; justify-content: space-between; }`]
})
export class RoomsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  readonly sedeId = input.required({ transform: numberAttribute });
  protected readonly todasSalas = signal<SalaEstudioDTO[]>([]);
  protected fechaInicio = '';
  protected fechaFin = '';

  protected readonly salasSede = computed(() =>
    this.todasSalas().filter((s) => s.sedeId === this.sedeId() && s.activa)
  );

  ngOnInit(): void {
    this.ops.salasPorSede(this.sedeId()).subscribe((s) => this.todasSalas.set(s));
  }

  protected reservar(salaId: number): void {
    if (!this.auth.autenticado()) { this.router.navigate(['/login']); return; }
    if (!this.fechaInicio || !this.fechaFin) {
      this.ui.mostrar('Selecciona fecha y hora de inicio y fin.', 'advertencia');
      return;
    }
    this.ops.crearReservaSala({
      usuarioId: this.auth.usuarioId()!,
      salaId,
      fechaInicio: this.fechaInicio,
      fechaFin: this.fechaFin,
    }).subscribe({
      next: (res) => {
        if (res.estado === 'PENDIENTE_PAGO' && res.valorTotal && res.valorTotal > 0) {
          this.router.navigate(['/pago', 'RESERVA_SALA', res.id], { queryParams: { monto: res.valorTotal } });
        } else {
          this.ui.mostrar('Sala reservada exitosamente.', 'exito');
          this.router.navigate(['/mi-cuenta/salas']);
        }
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
