import { Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { MainTemplate } from '../main-template/main-template';

@Component({
  selector: 'app-panel-template',
  imports: [MainTemplate, RouterLink, RouterLinkActive],
  template: `
    <app-main-template [titulo]="titulo()" [subtitulo]="subtitulo()">
      <div class="panel-grid">
        <aside class="panel-side" aria-label="Navegación de gestión">
          @for (item of menu(); track item.ruta) {
            <a [routerLink]="item.ruta" routerLinkActive="active">{{ item.texto }}</a>
          }
        </aside>
        <section class="panel-body">
          <ng-content />
        </section>
      </div>
    </app-main-template>
  `,
  styles: [`
    .panel-grid { display: grid; grid-template-columns: 245px 1fr; gap: 1.5rem; align-items: start; }
    @media (max-width: 820px) { .panel-grid { grid-template-columns: 1fr; } }
    .panel-side {
      background: var(--color-superficie); border: 1px solid var(--color-borde); border-radius: var(--radio-lg);
      padding: 0.75rem; display: flex; flex-direction: column; gap: 0.25rem;
      a { text-decoration: none; color: var(--color-texto); padding: 0.55rem 0.85rem; border-radius: var(--radio); font-weight: 600;
          &:hover, &.active { background: var(--color-primario-suave); color: var(--color-primario); } }
    }
  `]
})
export class PanelTemplate {
  private readonly auth = inject(AuthService);
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>();

  protected readonly menu = computed(() => {
    const rol = this.auth.rol();
    if (rol === 'BIBLIOTECARIO') {
      return [
        { ruta: '/panel', texto: 'Resumen Sede' },
        { ruta: '/panel/prestamos', texto: 'Préstamos y Pedidos' },
        { ruta: '/panel/reservas', texto: 'Reservas de Libros' },
        { ruta: '/panel/devoluciones', texto: 'Confirmar Devolución (QR)' },
        { ruta: '/panel/libros', texto: 'Catálogo, Autores y Géneros' },
        { ruta: '/panel/ejemplares', texto: 'Ejemplares y Stock' },
        { ruta: '/panel/salas', texto: 'Salas de Estudio' },
        { ruta: '/panel/eventos', texto: 'Eventos de Sede' },
      ];
    }
    if (rol === 'ADMIN') {
      return [
        { ruta: '/admin/usuarios', texto: 'Usuarios y Personal' },
        { ruta: '/admin/sedes', texto: 'Sedes / Bibliotecas' },
        { ruta: '/admin/planes', texto: 'Planes Suscripción' },
        { ruta: '/admin/politicas', texto: 'Políticas y Plazos' },
        { ruta: '/admin/tarifas-envio', texto: 'Tarifas Interciudad' },
        { ruta: '/admin/pagos', texto: 'Pagos y Reembolsos' },
        { ruta: '/admin/reportes', texto: 'Reportes (PDF/CSV)' },
        { ruta: '/admin/auditoria', texto: 'Auditoría' },
        { ruta: '/admin/faq', texto: 'Base Chatbot (FAQ)' },
      ];
    }
    return [{ ruta: '/reparto', texto: 'Entregas y Retornos' }];
  });
}
