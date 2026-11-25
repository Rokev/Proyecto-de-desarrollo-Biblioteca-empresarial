import { Component, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { MainTemplate } from '../main-template/main-template';

@Component({
  selector: 'app-account-template',
  imports: [MainTemplate, RouterLink, RouterLinkActive],
  template: `
    <app-main-template [titulo]="titulo()">
      <div class="account-grid">
        <aside class="account-menu" aria-label="Menú de cuenta">
          <a routerLink="/mi-cuenta/prestamos" routerLinkActive="active">Mis Préstamos y Reservas</a>
          <a routerLink="/mi-cuenta/reservas" routerLinkActive="active">Mis Reservas de Libros</a>
          @if (auth.esSuscriptor()) {
            <a routerLink="/mi-cuenta/pedidos" routerLinkActive="active">Pedidos Interciudad</a>
            <a routerLink="/mi-cuenta/suscripcion" routerLinkActive="active">Mi Suscripción</a>
          } @else {
            <a routerLink="/planes" routerLinkActive="active">Hazte Suscriptor</a>
          }
          <a routerLink="/mi-cuenta/salas" routerLinkActive="active">Salas Reservadas</a>
          <a routerLink="/mi-cuenta/eventos" routerLinkActive="active">Mis Eventos</a>
          <a routerLink="/mi-cuenta/multas" routerLinkActive="active">Multas y Sanciones</a>
          <a routerLink="/mi-cuenta/pagos" routerLinkActive="active">Historial de Pagos</a>
          <a routerLink="/mi-cuenta/perfil" routerLinkActive="active">Mi Perfil</a>
        </aside>
        <section class="account-body">
          <ng-content />
        </section>
      </div>
    </app-main-template>
  `,
  styles: [`
    .account-grid { display: grid; grid-template-columns: 240px 1fr; gap: 1.5rem; align-items: start; }
    @media (max-width: 768px) { .account-grid { grid-template-columns: 1fr; } }
    .account-menu {
      background: var(--color-superficie); border: 1px solid var(--color-borde); border-radius: var(--radio-lg);
      padding: 0.75rem; display: flex; flex-direction: column; gap: 0.25rem;
      a { text-decoration: none; color: var(--color-texto); padding: 0.55rem 0.85rem; border-radius: var(--radio); font-weight: 600;
          &:hover, &.active { background: var(--color-primario-suave); color: var(--color-primario); } }
    }
  `]
})
export class AccountTemplate {
  protected readonly auth = inject(AuthService);
  readonly titulo = input.required<string>();
}
