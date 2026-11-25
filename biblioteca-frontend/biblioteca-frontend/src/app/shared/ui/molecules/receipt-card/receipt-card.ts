import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ComprobantePagoDTO } from '../../../../core/models/biblioteca.models';
import { Badge } from '../../atoms/badge/badge';
import { Card } from '../../atoms/card/card';

@Component({
  selector: 'app-receipt-card',
  imports: [Card, Badge, CurrencyPipe, DatePipe],
  template: `
    @let c = comprobante();
    <app-card [destacada]="c.estado === 'APROBADO'">
      <div cardEncabezado class="recibo__head">
        <strong>Comprobante {{ c.numeroReferencia }}</strong>
        <app-badge [texto]="c.estado" [estado]="c.estado" />
      </div>
      <dl class="recibo__grid">
        <dt>Concepto</dt><dd>{{ c.concepto }}</dd>
        <dt>Monto</dt><dd>{{ c.monto | currency:'COP':'symbol-narrow':'1.0-0' }}</dd>
        <dt>Tarjeta</dt><dd>•••• •••• •••• {{ c.ultimosCuatroDigitos }}</dd>
        <dt>Fecha</dt><dd>{{ c.fechaPago | date:'d MMM y, h:mm a' }}</dd>
        <dt>Detalle</dt><dd>{{ c.mensaje }}</dd>
      </dl>
    </app-card>
  `,
  styles: [`
    .recibo__head { display: flex; justify-content: space-between; align-items: center; }
    .recibo__grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.4rem 0.75rem; margin: 0; dt { font-weight: 700; color: var(--color-texto-apoyo); } dd { margin: 0; } }
  `]
})
export class ReceiptCard {
  readonly comprobante = input.required<ComprobantePagoDTO>();
}
