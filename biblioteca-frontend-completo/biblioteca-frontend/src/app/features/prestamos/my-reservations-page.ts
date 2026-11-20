import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ReservaLibroDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-reservations-page',
  imports: [DatePipe, AccountTemplate, Card, Badge, Button, EmptyState],
  template: `
    <app-account-template titulo="Mis Reservas de Libros">
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
                  <app-button texto="Cancelar Reserva" variante="peligro" tamano="pequeno" (presionado)="cancelar(r.id!)" />
                </div>
              }
            </app-card>
          }
        </div>
      }
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class MyReservationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly reservas = signal<ReservaLibroDTO[]>([]);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ops.reservasLibroUsuario(this.auth.usuarioId()!).subscribe((r) => this.reservas.set(r));
  }

  protected cancelar(id: number): void {
    this.ops.cancelarReservaLibro(id).subscribe({
      next: () => { this.ui.mostrar('Reserva cancelada.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
