import { CurrencyPipe } from '@angular/common';
import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ComprobantePagoDTO, SolicitudPagoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { ReceiptCard } from '../../shared/ui/molecules/receipt-card/receipt-card';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-payment-page',
  imports: [FormsModule, CurrencyPipe, MainTemplate, Card, Alert, FormField, Button, ReceiptCard],
  template: `
    <app-main-template titulo="Pasarela de Pago Simulada">
      @if (error()) { <app-alert titulo="Error al procesar" [mensaje]="error()!" tipo="error" /> }

      @if (comprobantes().length > 0) {
        <div class="grid-cards">
          @for (c of comprobantes(); track c.numeroReferencia) {
            <app-receipt-card [comprobante]="c" />
          }
        </div>
        <div style="margin-top: 1.25rem;">
          <app-button texto="Continuar" (presionado)="volver()" />
        </div>
      } @else {
        <app-card>
          <p><strong>Concepto:</strong> {{ tipo() }}</p>
          <p><strong>Total a pagar:</strong> {{ montoNumerico() | currency:'COP':'symbol-narrow':'1.0-0' }}</p>

          <div class="helper-tarjetas">
            <strong>Tarjetas de prueba predefinidas:</strong>
            <div class="chips">
              <button type="button" (click)="numeroTarjeta = '4111111111111111'">Aprobada Visa (4111...1111)</button>
              <button type="button" (click)="numeroTarjeta = '5555555555554444'">Aprobada MC (5555...4444)</button>
              <button type="button" (click)="numeroTarjeta = '4000000000000002'">Rechazada (4000...0002)</button>
            </div>
          </div>

          <form (ngSubmit)="pagar()">
            <app-form-field etiqueta="Número de tarjeta simulada" para="cardNum" [obligatorio]="true" ayuda="No almacenamos datos reales; solo los últimos 4 dígitos.">
              <input id="cardNum" class="campo" [(ngModel)]="numeroTarjeta" name="cardNum" maxlength="19" required />
            </app-form-field>
            <app-button tipo="submit" texto="Confirmar Pago" [cargando]="procesando()" />
          </form>
        </app-card>
      }
    </app-main-template>
  `,
  styles: [`
    .helper-tarjetas { background: #f1f5f9; padding: 0.75rem; border-radius: var(--radio); margin-bottom: 1rem; font-size: 0.85rem; }
    .chips { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.4rem; button { border: 1px solid var(--color-primario); background: #fff; color: var(--color-primario); border-radius: var(--radio); padding: 0.25rem 0.6rem; cursor: pointer; font-weight: 600; } }
  `]
})
export class PaymentPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly tipo = input.required<SolicitudPagoDTO['concepto']>();
  readonly id = input.required({ transform: numberAttribute });
  readonly monto = input<string>('0');
  readonly ids = input<string>();

  protected numeroTarjeta = '4111111111111111';
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly comprobantes = signal<ComprobantePagoDTO[]>([]);
  protected readonly multasDetalle = signal<Map<number, number>>(new Map());

  protected montoNumerico(): number { return Number(this.monto() || 0); }

  ngOnInit(): void {
    if (this.tipo() === 'MULTA' || this.tipo() === 'RECARGO') {
      this.ops.multasUsuario(this.auth.usuarioId()!).subscribe((list) => {
        const mapa = new Map<number, number>();
        list.forEach((m) => mapa.set(m.id, m.monto));
        this.multasDetalle.set(mapa);
      });
    }
  }

  protected async pagar(): Promise<void> {
    const uid = this.auth.usuarioId()!;
    const tarjeta = this.numeroTarjeta.replace(/\s+/g, '');
    this.procesando.set(true);
    this.error.set(null);

    try {
      const listaComprobantes: ComprobantePagoDTO[] = [];
      if ((this.tipo() === 'MULTA' || this.tipo() === 'RECARGO') && this.ids()) {
        const listaIds = this.ids()!.split(',').map(Number);
        for (const mId of listaIds) {
          const montoIndiv = this.multasDetalle().get(mId) ?? this.montoNumerico();
          const comp = await firstValueFrom(this.ops.procesarPago({
            usuarioId: uid,
            concepto: this.tipo(),
            monto: montoIndiv,
            numeroTarjeta: tarjeta,
            multaId: mId,
          }));
          listaComprobantes.push(comp);
        }
      } else {
        const payload: SolicitudPagoDTO = {
          usuarioId: uid,
          concepto: this.tipo(),
          monto: this.montoNumerico(),
          numeroTarjeta: tarjeta,
        };
        if (this.tipo() === 'MULTA' || this.tipo() === 'RECARGO') payload.multaId = this.id();
        if (this.tipo() === 'SUSCRIPCION') payload.planSuscripcionId = this.id();
        if (this.tipo() === 'RESERVA_SALA') payload.reservaSalaId = this.id();
        if (this.tipo() === 'ENVIO_INTERCIUDAD') payload.pedidoInterciudadId = this.id();

        const comp = await firstValueFrom(this.ops.procesarPago(payload));
        listaComprobantes.push(comp);
      }

      this.comprobantes.set(listaComprobantes);
      if (this.tipo() === 'SUSCRIPCION') {
        this.auth.refrescarSuscripcion(uid);
      }
    } catch (e) {
      this.error.set(extraerMensajeError(e));
    } finally {
      this.procesando.set(false);
    }
  }

  protected volver(): void {
    switch (this.tipo()) {
      case 'SUSCRIPCION': this.router.navigate(['/mi-cuenta/suscripcion']); break;
      case 'ENVIO_INTERCIUDAD': this.router.navigate(['/mi-cuenta/pedidos']); break;
      case 'RESERVA_SALA': this.router.navigate(['/mi-cuenta/salas']); break;
      default: this.router.navigate(['/mi-cuenta/multas']);
    }
  }
}
