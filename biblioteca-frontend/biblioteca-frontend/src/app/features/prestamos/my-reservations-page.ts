import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ReservaLibroDTO } from '../../core/models/biblioteca.models';
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
  selector: 'app-my-reservations-page',
  imports: [RouterLink, DatePipe, AccountTemplate, Card, Badge, Button, EmptyState, ConfirmDialog],
  template: `
    <app-account-template titulo="Mis Reservas de Libros">
      <p><a routerLink="/mi-cuenta/prestamos">← Ver Mis Préstamos y Reservas</a></p>
      @if (reservas().length === 0) {
        <app-empty-state titulo="No tienes reservas" descripcion="Cuando un libro no tenga stock en una sede podrás reservarlo desde su ficha." />
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
                <p><strong>Retirar antes de:</strong> {{ r.fechaLimiteRetiro | date:'d MMM y, h:mm a' }}</p>
              }
              @if (r.estado === 'EN_ESPERA' || r.estado === 'NOTIFICADA') {
                <div cardPie>
                  <app-button texto="Cancelar Reserva" variante="peligro" tamano="pequeno" (presionado)="pedirCancelar(r.id!)" />
                </div>
              }
            </app-card>
          }
        </div>
      }

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Cancelar Reserva"
        mensaje="¿Confirmas que deseas liberar esta reserva?"
        textoConfirmar="Confirmar Cancelación"
        [peligro]="true"
        (confirmar)="ejecutarCancelar()"
      />
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class MyReservationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly reservas = signal<ReservaLibroDTO[]>([]);
  protected readonly confirmOpen = signal(false);
  protected readonly idSel = signal<number | null>(null);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ops.reservasLibroUsuario(this.auth.usuarioId()!).subscribe((r) => this.reservas.set(r));
  }

  protected pedirCancelar(id: number): void {
    this.idSel.set(id);
    this.confirmOpen.set(true);
  }

  protected ejecutarCancelar(): void {
    const id = this.idSel();
    if (!id) return;
    this.ops.cancelarReservaLibro(id).subscribe({
      next: () => { this.ui.mostrar('Reserva cancelada.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
