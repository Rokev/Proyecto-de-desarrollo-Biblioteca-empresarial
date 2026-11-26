import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MultaDTO, PrestamoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-loan-detail-page',
  imports: [RouterLink, DatePipe, CurrencyPipe, PanelTemplate, Card, Badge, Button, ConfirmDialog],
  template: `
    <app-panel-template titulo="Detalle de Préstamo">
      <p><a routerLink="/panel/prestamos">← Volver al listado</a></p>
      @if (prestamo(); as p) {
        <app-card>
          <div cardEncabezado style="display:flex; justify-content:space-between; align-items:center;">
            <strong>Préstamo #{{ p.id }}</strong>
            <app-badge [texto]="p.estado || ''" [estado]="p.estado || ''" />
          </div>
          <p><strong>Usuario ID:</strong> #{{ p.usuarioId }} · <strong>Ejemplar ID:</strong> #{{ p.ejemplarId }}</p>
          <p><strong>Modalidad:</strong> {{ p.modalidad }} · <strong>Sede retiro:</strong> #{{ p.sedeRetiroId }}</p>
          <p><strong>Solicitado:</strong> {{ p.fechaSolicitud | date:'d MMM y, h:mm a' }}</p>
          <p><strong>Fecha Entrega:</strong> {{ p.fechaEntrega ? (p.fechaEntrega | date:'d MMM y, h:mm a') : 'Pendiente de retiro' }}</p>
          <p><strong>Fecha Vencimiento:</strong> {{ p.fechaVencimiento ? (p.fechaVencimiento | date:'d MMM y') : 'Se inicia al entregar' }}</p>
          <div cardPie>
            @if (p.estado === 'SOLICITADO') {
              <app-button texto="Registrar Entrega (Retiro Mostrador)" (presionado)="abrirConfirm('ENTREGAR')" />
            }
            @if (p.estado === 'ACTIVO' || p.estado === 'VENCIDO') {
              <app-button texto="Registrar como Dañado" variante="peligro" (presionado)="abrirConfirm('DANADO')" />
              <app-button texto="Registrar como Perdido" variante="peligro" (presionado)="abrirConfirm('PERDIDO')" />
            }
          </div>
        </app-card>

        <h3 style="margin-top: 1.5rem;">Multas del Usuario (#{{ p.usuarioId }})</h3>
        <div class="tabla-responsive">
          <table class="tabla">
            <thead><tr><th>ID</th><th>Motivo</th><th>Monto</th><th>Estado</th></tr></thead>
            <tbody>
              @for (m of multasUsuario(); track m.id) {
                <tr><td>#{{ m.id }}</td><td>{{ m.motivo }}</td><td>{{ m.monto | currency:'COP':'symbol-narrow':'1.0-0' }}</td><td><app-badge [texto]="m.estado" [estado]="m.estado" /></td></tr>
              } @empty {
                <tr><td colspan="4">Sin multas registradas.</td></tr>
              }
            </tbody>
          </table>
        </div>
      }

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="confirmTitulo()"
        [mensaje]="confirmMensaje()"
        [peligro]="accionPendiente() !== 'ENTREGAR'"
        [mostrarInput]="accionPendiente() !== 'ENTREGAR'"
        etiquetaInput="Observaciones del daño o pérdida"
        (confirmar)="ejecutarAccion($event)"
      />
    </app-panel-template>
  `
})
export class StaffLoanDetailPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  readonly id = input.required({ transform: numberAttribute });
  protected readonly prestamo = signal<PrestamoDTO | null>(null);
  protected readonly multasUsuario = signal<MultaDTO[]>([]);

  protected readonly confirmOpen = signal(false);
  protected readonly accionPendiente = signal<'ENTREGAR' | 'DANADO' | 'PERDIDO'>('ENTREGAR');
  protected readonly confirmTitulo = signal('');
  protected readonly confirmMensaje = signal('');

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ops.obtenerPrestamo(this.id()).subscribe((p) => {
      this.prestamo.set(p);
      this.ops.multasUsuario(p.usuarioId).subscribe((m) => this.multasUsuario.set(m));
    });
  }

  protected abrirConfirm(acc: 'ENTREGAR' | 'DANADO' | 'PERDIDO'): void {
    this.accionPendiente.set(acc);
    if (acc === 'ENTREGAR') {
      this.confirmTitulo.set('Confirmar Entrega de Libro');
      this.confirmMensaje.set('¿Confirmas la entrega física del libro al usuario? Esto iniciará el plazo de devolución.');
    } else {
      this.confirmTitulo.set(`Registrar Libro como ${acc}`);
      this.confirmMensaje.set(`Se descontará del stock disponible y se aplicará la multa configurada por ${acc.toLowerCase()}.`);
    }
    this.confirmOpen.set(true);
  }

  protected ejecutarAccion(obs: string): void {
    const p = this.prestamo();
    if (!p) return;
    const acc = this.accionPendiente();
    if (acc === 'ENTREGAR') {
      this.ops.actualizarPrestamo(p.id!, { ...p, estado: 'ACTIVO' }).subscribe({
        next: () => { this.ui.mostrar('Entrega registrada. Plazo iniciado.', 'exito'); this.cargar(); },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else {
      this.ops.registrarDanioPerdida(p.id!, acc, this.auth.usuarioId()!, obs || undefined).subscribe({
        next: () => { this.ui.mostrar(`Libro marcado como ${acc} y multa generada.`, 'advertencia'); this.cargar(); },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    }
  }
}
