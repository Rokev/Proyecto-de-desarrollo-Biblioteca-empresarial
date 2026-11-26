import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PrestamoDetalleDTO, ReservaLibroDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-loans-page',
  imports: [RouterLink, DatePipe, AccountTemplate, Card, Badge, Button, EmptyState, ConfirmDialog],
  template: `
    <app-account-template titulo="Mis Préstamos y Reservas">
      <h2>Préstamos</h2>
      @if (prestamos().length === 0) {
        <app-empty-state titulo="Aún no tienes préstamos" descripcion="Explora el catálogo y solicita tu primer libro." />
      } @else {
        <div class="grid-cards">
          @for (p of prestamos(); track p.prestamoId) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>{{ p.libroTitulo }}</strong>
                <app-badge [texto]="p.estado || 'SOLICITADO'" [estado]="p.estado || 'SOLICITADO'" />
              </div>
              <p><strong>Sede retiro:</strong> {{ p.sedeNombre }} · <strong>Modalidad:</strong> {{ p.modalidad }}</p>
              <p><strong>Solicitado:</strong> {{ p.fechaSolicitud | date:'d MMM y' }}</p>
              @if (p.fechaLimiteDevolucion) {
                <p><strong>Fecha límite devolución:</strong> {{ p.fechaLimiteDevolucion | date:'d MMM y' }}</p>
              }
              @if (p.estado === 'ACTIVO' || p.estado === 'VENCIDO') {
                <div cardPie>
                  <a [routerLink]="['/mi-cuenta/prestamos', p.prestamoId, 'devolver']" class="link-dev">Generar QR de devolución →</a>
                </div>
              }
            </app-card>
          }
        </div>
      }

      <h2 style="margin-top: 2rem;">Reservas de Libros <a routerLink="/mi-cuenta/reservas" style="font-size: 0.9rem; font-weight: 600;">(Ver solo reservas)</a></h2>
      @if (reservas().length === 0) {
        <p style="color: var(--color-texto-apoyo);">No tienes reservas de libros registradas.</p>
      } @else {
        <div class="grid-cards">
          @for (r of reservas(); track r.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>Reserva #{{ r.id }} (Libro #{{ r.libroId }})</strong>
                <app-badge [texto]="r.estado || 'EN_ESPERA'" [estado]="r.estado || 'EN_ESPERA'" />
              </div>
              <p><strong>Sede:</strong> #{{ r.sedeId }} · <strong>Fecha:</strong> {{ r.fechaReserva | date:'d MMM y' }}</p>
              @if (r.fechaLimiteRetiro) {
                <p><strong>Fecha límite retiro:</strong> {{ r.fechaLimiteRetiro | date:'d MMM y, h:mm a' }}</p>
              }
              @if (r.estado === 'EN_ESPERA' || r.estado === 'NOTIFICADA') {
                <div cardPie>
                  <app-button texto="Cancelar Reserva" variante="peligro" tamano="pequeno" (presionado)="pedirCancelarReserva(r.id!)" />
                </div>
              }
            </app-card>
          }
        </div>
      }

      <app-confirm-dialog
        [(abierto)]="confirmCancelOpen"
        titulo="Cancelar Reserva"
        mensaje="¿Estás seguro de que deseas cancelar esta reserva de libro?"
        textoConfirmar="Sí, cancelar"
        [peligro]="true"
        (confirmar)="ejecutarCancelarReserva()"
      />
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; } .link-dev { color: var(--color-primario); font-weight: 700; text-decoration: none; }`]
})
export class MyLoansPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly prestamos = signal<PrestamoDetalleDTO[]>([]);
  protected readonly reservas = signal<ReservaLibroDTO[]>([]);
  protected readonly confirmCancelOpen = signal(false);
  protected readonly reservaACancelar = signal<number | null>(null);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const uid = this.auth.usuarioId()!;
    this.ops.prestamosPorUsuario(uid).subscribe((res) => this.prestamos.set(res));
    this.ops.reservasLibroUsuario(uid).subscribe((res) => this.reservas.set(res));
  }

  protected pedirCancelarReserva(id: number): void {
    this.reservaACancelar.set(id);
    this.confirmCancelOpen.set(true);
  }

  protected ejecutarCancelarReserva(): void {
    const id = this.reservaACancelar();
    if (!id) return;
    this.ops.cancelarReservaLibro(id).subscribe({
      next: () => { this.ui.mostrar('Reserva cancelada.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
