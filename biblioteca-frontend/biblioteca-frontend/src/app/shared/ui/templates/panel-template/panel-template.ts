import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { SedeDTO } from '../../../../core/models/biblioteca.models';
import { AuthService } from '../../../../core/services/auth.service';
import { CatalogoService } from '../../../../core/services/catalogo.service';
import { MainTemplate } from '../main-template/main-template';

@Component({
  selector: 'app-panel-template',
  imports: [MainTemplate, RouterLink, RouterLinkActive],
  template: `
    <app-main-template [titulo]="titulo()" [subtitulo]="subtitulo()">
      <div class="panel-grid">
        <aside class="panel-side" aria-label="Navegación de gestión">
          @if (auth.rol() === 'BIBLIOTECARIO') {
            <div class="sede-info">
              <small>Sede asignada</small>
              @if (sedeActual(); as s) {
                <strong>{{ s.nombre }}</strong>
                <span>{{ s.ciudad }}</span>
              } @else if (auth.perfil()?.sedeId) {
                <strong>Sede #{{ auth.perfil()?.sedeId }}</strong>
              } @else {
                <strong>Sin sede asignada</strong>
              }
            </div>
          }
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
    .sede-info {
      background: #eff6ff; border: 1px solid #bfdbfe; border-radius: var(--radio);
      padding: 0.65rem 0.85rem; margin-bottom: 0.5rem; display: flex; flex-direction: column; gap: 0.1rem;
      small { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: #1d4ed8; font-weight: 700; }
      strong { font-size: 0.92rem; color: #0f172a; }
      span { font-size: 0.82rem; color: #475569; }
    }
  `]
})
export class PanelTemplate {
  protected readonly auth = inject(AuthService);
  private readonly catalogo = inject(CatalogoService);
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>();

  protected readonly sedeActual = signal<SedeDTO | null>(null);
  private ultimoSedeIdCargado: number | null = null;

  constructor() {
    effect(() => {
      const rol = this.auth.rol();
      const sedeId = this.auth.perfil()?.sedeId;
      if (rol !== 'BIBLIOTECARIO' || !sedeId) {
        return;
      }
      const idNum = Number(sedeId);
      if (this.ultimoSedeIdCargado === idNum && this.sedeActual()) {
        return;
      }
      this.ultimoSedeIdCargado = idNum;
      this.catalogo.listarSedesActivas().subscribe({
        next: (sedes) => {
          const encontrada = sedes.find((s) => Number(s.id) === idNum);
          if (encontrada) {
            this.sedeActual.set(encontrada);
          } else {
            this.catalogo.listarTodasSedes().subscribe({
              next: (todas) => this.sedeActual.set(todas.find((s) => Number(s.id) === idNum) ?? null),
              error: () => {},
            });
          }
        },
        error: () => {
          this.catalogo.listarTodasSedes().subscribe({
            next: (todas) => this.sedeActual.set(todas.find((s) => Number(s.id) === idNum) ?? null),
            error: () => {},
          });
        },
      });
    });
  }

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