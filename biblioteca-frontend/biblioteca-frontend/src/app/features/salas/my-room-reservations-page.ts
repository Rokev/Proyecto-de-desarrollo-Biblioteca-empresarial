import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ReservaSalaDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-room-reservations-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, AccountTemplate, Card, Badge, Button, ConfirmDialog],
  template: `
    <app-account-template titulo="Mis Reservas de Salas">
      <div class="grid-cards">
        @for (r of reservas(); track r.id) {
          <app-card>
            <div cardEncabezado class="head">
              <strong>Reserva Sala #{{ r.salaId }}</strong>
              <app-badge [texto]="r.estado || ''" [estado]="r.estado || ''" />
            </div>
            <p><strong>Inicio:</strong> {{ r.fechaInicio | date:'d MMM y, h:mm a' }}</p>
            <p><strong>Fin:</strong> {{ r.fechaFin | date:'d MMM y, h:mm a' }}</p>
            <p><strong>Total:</strong> {{ r.valorTotal | currency:'COP':'symbol-narrow':'1.0-0' }} (Desc: {{ r.descuento | currency:'COP':'symbol-narrow':'1.0-0' }})</p>
            <div cardPie>
              @if (r.estado === 'PENDIENTE_PAGO') {
                <a [routerLink]="['/pago', 'RESERVA_SALA', r.id]" [queryParams]="{ monto: r.valorTotal }">Pagar ahora</a>
              }
              @if (r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE_PAGO') {
                <app-button texto="Cancelar" variante="peligro" tamano="pequeno" (presionado)="pedirCancelar(r.id!)" />
              }
            </div>
          </app-card>
        }
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Cancelar Reserva de Sala"
        mensaje="¿Confirmas que deseas cancelar esta reserva y liberar la sala?"
        textoConfirmar="Sí, cancelar"
        [peligro]="true"
        (confirmar)="ejecutarCancelar()"
      />
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class MyRoomReservationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly reservas = signal<ReservaSalaDTO[]>([]);
  protected readonly confirmOpen = signal(false);
  protected readonly idSel = signal<number | null>(null);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const uid = this.auth.usuarioId()!;
    this.ops.listarReservasSalas().subscribe((all) =>
      this.reservas.set(all.filter((r) => r.usuarioId === uid))
    );
  }

  protected pedirCancelar(id: number): void {
    this.idSel.set(id);
    this.confirmOpen.set(true);
  }

  protected ejecutarCancelar(): void {
    const id = this.idSel();
    if (!id) return;
    this.ops.cancelarReservaSala(id).subscribe(() => {
      this.ui.mostrar('Reserva de sala cancelada.', 'info');
      this.cargar();
    });
  }
}
