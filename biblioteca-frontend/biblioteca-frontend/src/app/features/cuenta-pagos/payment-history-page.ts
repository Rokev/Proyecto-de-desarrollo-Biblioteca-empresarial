import { Component, inject, OnInit, signal } from '@angular/core';
import { ComprobantePagoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { ReceiptCard } from '../../shared/ui/molecules/receipt-card/receipt-card';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-payment-history-page',
  imports: [AccountTemplate, ReceiptCard, EmptyState],
  template: `
    <app-account-template titulo="Historial de Pagos y Comprobantes">
      @if (pagos().length === 0) {
        <app-empty-state titulo="Sin pagos registrados" descripcion="Aquí verás tus comprobantes de suscripción, envíos, salas y multas." />
      } @else {
        <div class="grid-cards">
          @for (p of pagos(); track p.numeroReferencia) {
            <app-receipt-card [comprobante]="p" />
          }
        </div>
      }
    </app-account-template>
  `
})
export class PaymentHistoryPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  protected readonly pagos = signal<ComprobantePagoDTO[]>([]);

  ngOnInit(): void {
    this.ops.pagosUsuario(this.auth.usuarioId()!).subscribe((p) => this.pagos.set(p));
  }
}
