import { CurrencyPipe } from '@angular/common';
import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DisponibilidadSalasDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { StatCard } from '../../shared/ui/molecules/stat-card/stat-card';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-rooms-page',
  imports: [FormsModule, CurrencyPipe, MainTemplate, Card, Badge, Button, EmptyState, Alert, StatCard],
  template: `
    <app-main-template titulo="Salas de Estudio" subtitulo="Consulta disponibilidad horaria y reserva tu espacio.">
      <app-card>
        <div class="rango">
          <label>Inicio: <input type="datetime-local" class="campo" [(ngModel)]="fechaInicio" (change)="consultarDisponibilidad()" /></label>
          <label>Fin: <input type="datetime-local" class="campo" [(ngModel)]="fechaFin" (change)="consultarDisponibilidad()" /></label>
          <app-button texto="Actualizar Disponibilidad" variante="contorno" (presionado)="consultarDisponibilidad()" />
        </div>
      </app-card>

      @if (disponibilidad(); as d) {
        <div class="grid-cards" style="margin: 1.25rem 0;">
          <app-stat-card etiqueta="Salas Totales en Sede" [valor]="d.totalSalas" />
          <app-stat-card etiqueta="Salas Libres en Horario" [valor]="d.salasDisponibles" />
          @if (auth.esSuscriptor() && auth.suscripcionVigente(); as sub) {
            <app-stat-card etiqueta="Tu Descuento de Suscriptor" [valor]="sub.descuentoSalas + '%'" />
          }
        </div>

        @if (!d.tieneSalas) {
          <app-empty-state titulo="Sin salas disponibles" descripcion="Esta sede no cuenta con salas de estudio activas." />
        } @else {
          <div class="grid-cards">
            @for (s of d.salas; track s.salaId) {
              <app-card>
                <div cardEncabezado class="head">
                  <strong>{{ s.nombre }}</strong>
                  <app-badge [texto]="s.disponible ? 'Libre' : 'Ocupada'" [estado]="s.disponible ? 'DISPONIBLE' : 'VENCIDO'" />
                </div>
                <p><strong>Capacidad:</strong> {{ s.capacidad }} personas</p>
                <p><strong>Tarifa base:</strong> {{ s.tarifaPorHora | currency:'COP':'symbol-narrow':'1.0-0' }} / hora</p>
                @if (calcularEstimado(s.tarifaPorHora); as est) {
                  <app-alert
                    titulo="Resumen estimado de reserva"
                    [mensaje]="'Subtotal: $' + est.base + ' | Descuento: -$' + est.desc + ' | Total a pagar: $' + est.total"
                    tipo="info"
                  />
                }
                <div cardPie>
                  <app-button texto="Reservar Sala" [deshabilitado]="!s.disponible" (presionado)="reservar(s.salaId)" />
                </div>
              </app-card>
            }
          </div>
        }
      }
    </app-main-template>
  `,
  styles: [`.rango { display: flex; gap: 1rem; flex-wrap: wrap; align-items: flex-end; } .head { display: flex; justify-content: space-between; }`]
})
export class RoomsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  readonly sedeId = input.required({ transform: numberAttribute });
  protected readonly disponibilidad = signal<DisponibilidadSalasDTO | null>(null);
  protected fechaInicio = '';
  protected fechaFin = '';

  ngOnInit(): void {
    this.consultarDisponibilidad();
  }

  protected consultarDisponibilidad(): void {
    this.ops.disponibilidadSalas(
      this.sedeId(),
      this.fechaInicio || undefined,
      this.fechaFin || undefined
    ).subscribe((d) => this.disponibilidad.set(d));
  }

  protected calcularEstimado(tarifaHora: number): { base: number; desc: number; total: number } | null {
    if (!this.fechaInicio || !this.fechaFin) return null;
    const ms = new Date(this.fechaFin).getTime() - new Date(this.fechaInicio).getTime();
    if (ms <= 0) return null;
    const horas = ms / 3600000;
    const base = Math.round(tarifaHora * horas);
    const pct = this.auth.esSuscriptor() ? (this.auth.suscripcionVigente()?.descuentoSalas ?? 0) : 0;
    const desc = Math.round(base * (pct / 100));
    return { base, desc, total: base - desc };
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
