import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PrestamoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Card } from '../../shared/ui/atoms/card/card';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-loans-page',
  imports: [RouterLink, DatePipe, AccountTemplate, Card, Badge, EmptyState],
  template: `
    <app-account-template titulo="Mis Préstamos">
      @if (prestamos().length === 0) {
        <app-empty-state titulo="Aún no tienes préstamos" descripcion="Explora el catálogo y solicita tu primer libro." />
      } @else {
        <div class="grid-cards">
          @for (p of prestamos(); track p.id) {
            <app-card>
              <div cardEncabezado class="head">
                <strong>Préstamo #{{ p.id }}</strong>
                <app-badge [texto]="p.estado || 'SOLICITADO'" [estado]="p.estado || 'SOLICITADO'" />
              </div>
              <p><strong>Ejemplar ID:</strong> {{ p.ejemplarId }} · <strong>Modalidad:</strong> {{ p.modalidad }}</p>
              <p><strong>Solicitado:</strong> {{ p.fechaSolicitud | date:'d MMM y' }}</p>
              @if (p.fechaVencimiento) {
                <p><strong>Fecha límite devolución:</strong> {{ p.fechaVencimiento | date:'d MMM y' }}</p>
              }
              @if (p.estado === 'ACTIVO' || p.estado === 'VENCIDO') {
                <div cardPie>
                  <a [routerLink]="['/mi-cuenta/prestamos', p.id, 'devolver']" class="link-dev">Generar QR de devolución →</a>
                </div>
              }
            </app-card>
          }
        </div>
      }
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; } .link-dev { color: var(--color-primario); font-weight: 700; text-decoration: none; }`]
})
export class MyLoansPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);

  protected readonly prestamos = signal<PrestamoDTO[]>([]);

  ngOnInit(): void {
    this.ops.prestamosPorUsuario(this.auth.usuarioId()!).subscribe((res) => this.prestamos.set(res));
  }
}
